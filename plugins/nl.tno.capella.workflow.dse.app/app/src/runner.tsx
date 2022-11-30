/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import {PVMT, isRange, RunProgress, Run, PVMTExt, PropertyValueExt, FunctionalChainExt, ElementExt, PropertyValueGroupExt, PropertyValue, isPropertyValueExtNumber, isPropertyValueExtString, isPropertyValueExtEnum, isPropertyValueExtDurationViaResource, PropertyValueExtDurationViaResource} from './types';
import {Action} from './store';
import { ipcRenderer } from 'electron';
import React from 'react';
import {clone} from './utils';

function computeValues(pv: PropertyValueExt, pvg: PropertyValueGroupExt, pvmt: PVMT): string[] {
    if (pv.value == null) {
        return [];
    } else if (isPropertyValueExtNumber(pv)) {
        if (isRange(pv.value)) {
            let values = []
            let value = Number(pv.value.min);
            while (value <= Number(pv.value.max) && Number(pv.value.min) < Number(pv.value.max) && Number(pv.value.step) > 0) {
                values.push(value.toString());
                value += Number(pv.value.step);
            }
            return values;
        } else {
            return [pv.value];
        }
    } else if (isPropertyValueExtString(pv) || isPropertyValueExtEnum(pv)) {
        return pv.value;
    } else if (isPropertyValueExtDurationViaResource(pv)) {
        const resource = pvmt.functionalChains.flatMap((f) => f.elements.flatMap((e) => e.propertyValueGroups))
            .find((p) => p.id === pvg.id).propertyValues.find((p) => p.label === 'ResourceID');
        return [pv.value[resource.value].value];
    }
}

function addPropertyValue(pvmt: PVMT, fc: FunctionalChainExt, element: ElementExt, pvg: PropertyValueGroupExt, pv: PropertyValueExt, value: string): PVMT {
    pvmt = clone(pvmt);
    let functionalChainTarget = pvmt.functionalChains.find((i) => i.id === fc.id);
    if (!functionalChainTarget) {
        functionalChainTarget = {...fc, elements: []};
        pvmt.functionalChains.push(functionalChainTarget);
    }
    let elementTarget = functionalChainTarget.elements.find((i) => i.id === element.id);
    if (!elementTarget) {
        elementTarget = {...element, propertyValueGroups: []};
        functionalChainTarget.elements.push(elementTarget);
    }
    let pvgTarget = elementTarget.propertyValueGroups.find((i) => i.label === pvg.label);
    if (!pvgTarget) {
        pvgTarget = {...pvg, propertyValues: []}
        elementTarget.propertyValueGroups.push(pvgTarget);
    }
    let pvTarget: PropertyValue;
    if (isPropertyValueExtNumber(pv)) {
        pvTarget = {label: pv.label, value: Number(value), type: pv.type, id: pv.id};
    } else if (isPropertyValueExtString(pv)) {
        pvTarget = {label: pv.label, value, type: pv.type, id: pv.id};
    } else if (isPropertyValueExtDurationViaResource(pv)) {
        pvTarget = {label: pv.label, value: Number(value), type: pv.propertyValueExtNumber.type, id: pv.id};
    } else {
        pvTarget = {label: pv.label, value, type: pv.type, id: pv.id};
    }
    pvgTarget.propertyValues.push(pvTarget);
    return pvmt;
}

export async function run(pvmt: PVMTExt, runsPerPVMT: number, dispatch: React.Dispatch<Action>, cb: (progress: RunProgress) => void) {
    pvmt = clone(pvmt);

    // Compute possible values
    const propertyValueDurationViaResource: string[] = [];
    let combos: PVMT[] = [{functionalChains: [], name: pvmt.name}];
    pvmt.functionalChains.forEach((fc) => fc.elements.forEach((element) => element.propertyValueGroups.forEach((pvg) => 
        // Make sure Duration is computed last because of PropertyValueDurationViaResource
        pvg.propertyValues.sort((a) => a.label === 'Duration' ? 1 : -1).forEach((pv) => {
            let all: PVMT[] = [];
            for (let j = 0; j < combos.length; j++) {
                let values = computeValues(pv, pvg, combos[j]);
                if (values.length) {
                    for (let i = 0; i < values.length; i++) {
                        const newCombo = addPropertyValue(combos[j], fc, element, pvg, pv, values[i]);
                        all.push(newCombo);
                    }
                }
            }
            combos = all;

            if (pv.type.type === 'duration_via_resource') {
                propertyValueDurationViaResource.push(pv.id);
            }
        }
    ))));

    // Run!
    let progressUpdateInterval; 
    try {
        const start = Date.now();
        let counter = 0;
        for (const pvmt of combos) {
            const runs: Run[] = [];
            const existingRuns = await ipcRenderer.invoke('list-runs') as string[];
            const runId = existingRuns.length ? Math.max(...existingRuns.map(r => Number(r.split('-')[1]))) + 1 : 1;
            const name = `run-${runId}`;

            for (let i = 0; i < runsPerPVMT; i++) {
                const runStart = Date.now();
                const inProgressUpdate = () => {
                    const elapsed = ((Date.now() - runStart) / 1000).toFixed(0);
                    cb({state: 'in_progress', message: `Run ${counter + 1} out of ${combos.length * runsPerPVMT} (${elapsed}s)`});
                }
                inProgressUpdate();
                progressUpdateInterval = setInterval(inProgressUpdate, 1000);
                const run = await ipcRenderer.invoke('run', `${name}-${i + 1}`, pvmt) as Run;
                runs.push(run);
                clearInterval(progressUpdateInterval);
                counter++;
            }

            const averageTime = runs.map(r => r.time).reduce((a, b) => a + b, 0) / runs.length;
            const run = {pvmt, time: averageTime, name, propertyValueDurationViaResource};
            dispatch({action: 'add_run', run});
        }
        const elapsed = ((Date.now() - start) / 1000).toFixed(0);
        cb({state: 'done', message: `Finished, executed ${combos.length} run(s) in ${elapsed}s`});
    } catch (error) {
        const message = error.message.includes('run-canceled') ? 'Run(s) canceled' : `Run failed, with error '${error.message}'`;
        clearInterval(progressUpdateInterval);
        cb({state: 'error', message});
    }
}