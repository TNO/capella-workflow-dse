/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import {PVMTExt, PVMT} from './types';

export function clone<T>(toClone: T) : T {
    return JSON.parse(JSON.stringify(toClone));
}

export function pvmtToPVMTExtended(pvmt: PVMT) {
    const cloned = clone(pvmt) as unknown as PVMTExt;
    cloned.functionalChains.forEach((fc) => fc.elements.forEach((element) => element.propertyValueGroups.forEach((pvg) => pvg.propertyValues.forEach((pv) => {
        if (pv.type.type === 'string' || pv.type.type === 'enum') {
            // @ts-ignore
            pv.value = [pv.value]; 
        } else if (pv.value != null) {
            // @ts-ignore
            pv.value = pv.value.toString(); 
        }
    }))));
    return cloned;
}