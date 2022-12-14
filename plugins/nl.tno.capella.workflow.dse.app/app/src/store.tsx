/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React, {useReducer, createContext} from 'react';
import {
    PVMTExt, RunExt, Range, PropertyValueExt, PropertyValueExtString, PropertyValueExtNumber, PVMT, DefinitionEntry,
    PropertyValueExtDurationViaResource, isPropertyValueExtDurationViaResource} from './types';
import { ipcRenderer } from 'electron';
import { pvmtToPVMTExtended } from './utils';

interface State {
    svg: string, 
    pvmt: PVMTExt, 
    runs: RunExt[], 
    configurationItems: string[],
    definitions: DefinitionEntry[], 
    showRuns: boolean, 
    selectedElement: string | null, 
    highlightedElement: string | null,
    allResources: string[],
};
interface ActionUpdatePropertyValue {action: 'update_property_value', pv: PropertyValueExt, value: string | Range | string[]}
interface ActionAddRun {action: 'add_run', run: RunExt}
interface ActionClearRuns {action: 'clear_runs'}
interface ActionSetDefinitions {action: 'set_definitions', definitions: DefinitionEntry[]}
interface ActionShowRuns {action: 'show_runs', value: boolean}
interface ActionSetSelectedHighlightedElement {action: 'set_selected_element' | 'set_highlighted_element', id: string}
export type Action = ActionUpdatePropertyValue | ActionAddRun | ActionShowRuns | ActionSetSelectedHighlightedElement | ActionClearRuns | ActionSetDefinitions;

function getAllResources(state: State): string[] {
    let resources = state.pvmt.functionalChains.flatMap((fc) => fc.elements.flatMap((e) => e.propertyValueGroups
        .flatMap((pvg) => pvg.propertyValues.filter((pv) => pv.label === 'ResourceID').flatMap((pv: PropertyValueExtString) => pv.value)))) as string[];
    resources.push(...state.definitions.map((d) => d.resource));
    resources.push(...state.configurationItems.map((c) => `[CI] ${c}`));
    resources = resources.filter((r) => r);
    return Array.from(new Set(resources));
}

function updatePropertyValues(state: State) {
    // Translate Duration property value (PropertyValueNumberExtended/PropertyValueExtendedDurationViaResource)
    state.pvmt.functionalChains.forEach((fc) => fc.elements.filter((e) => e.type === 'FunctionalChainInvolvementFunction').forEach((e) => e.propertyValueGroups.forEach((pvg) => {
        const definitions = state.definitions.filter((d) => d.function === e.label);
        const resource = pvg.propertyValues.find((pv) => pv.label === 'ResourceID') as PropertyValueExtString;
        const duration = pvg.propertyValues.find((pv) => pv.label === 'Duration') as (PropertyValueExtNumber | PropertyValueExtDurationViaResource);
        if (!resource || !duration) return;
        const index = pvg.propertyValues.indexOf(duration);
        if (definitions.length && resource.value.length) { // PropertyValueExtendedDurationViaResource
            const value = Object.fromEntries(resource.value.map((r) => {
                const definition = definitions.find((d) => d.resource === r);
                return [r, {value: definition ? Number(definition.duration).toFixed(2) : '1.0', default: !definition}];
            }));
            if (isPropertyValueExtDurationViaResource(duration)) {
                pvg.propertyValues[index] = {...duration, value};
            } else { // PropertyValueNumberExtended -> PropertyValueExtendedDurationViaResource
                pvg.propertyValues[index] = {type: {type: 'duration_via_resource'}, label: duration.label, id: duration.id, value, propertyValueExtNumber: duration};
            }
        } else { // PropertyValueNumberExtended
            if (isPropertyValueExtDurationViaResource(duration)) { // PropertyValueExtendedDurationViaResource -> PropertyValueNumberExtended
                pvg.propertyValues[index] = duration.propertyValueExtNumber;
            }
        }
    })));
}

const ipcData: {svg: string, pvmt: PVMT, definitions: DefinitionEntry[], configurationItems: string[]} = ipcRenderer.sendSync('get-data');
const initialState: State = {
    svg: ipcData.svg, 
    pvmt: pvmtToPVMTExtended(ipcData.pvmt), 
    definitions: ipcData.definitions,
    configurationItems: ipcData.configurationItems,
    runs: [], showRuns: false, selectedElement: null, highlightedElement: null, allResources: [],
};
initialState.allResources = getAllResources(initialState);
updatePropertyValues(initialState);

export const Context = createContext<{state: State, dispatch: React.Dispatch<Action>}>(null);

export const Store = (props: {children: React.ReactNode}) => {
    const [state, dispatch] = useReducer((oldState: State, action: Action) => {
        const state: State = JSON.parse(JSON.stringify(oldState));
        if (action.action === 'update_property_value') {
            state.pvmt.functionalChains.forEach((fc) => fc.elements.forEach((e) => e.propertyValueGroups.forEach((pvg) => pvg.propertyValues.forEach((pv) => {
                if (pv.id === action.pv.id) {
                    if (pv.type.type === 'duration_via_resource') throw new Error(`Cannot change value of 'duration_via_resource' property value type`);
                    pv.value = action.value;
                }
            }))));
            updatePropertyValues(state);
            state.allResources = getAllResources(state);
        } else if (action.action === 'set_definitions') {
            state.definitions = action.definitions;
            state.allResources = getAllResources(state);
            updatePropertyValues(state);
        } else if (action.action === 'add_run') {
            state.runs.push(action.run);
        } else if (action.action === 'clear_runs') {
            state.runs = [];
        } else if (action.action === 'show_runs') {
            state.showRuns = action.value;
        } else if (action.action === 'set_selected_element') {
            state.selectedElement = action.id;
        } else if (action.action === 'set_highlighted_element') {
            state.highlightedElement = action.id;
        }
        return state;
    }, initialState);

    return (
        <Context.Provider value={{state, dispatch}}>
            {props.children}
        </Context.Provider>
    );
}