/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
export interface PropertyValueType {
    type: 'string' | 'int' | 'float' | 'enum';
};

export type RunMode = 'python' | 'poosl';

export interface RunProgress {
    state: 'error' | 'done' | 'in_progress',
    message: string,
}

export interface Run {
    name: string,
    time: number,
    pvmt: PVMT,
    mode: RunMode,
}

export interface RunExt extends Run {
    propertyValueDurationViaResource: string[],
}

export interface Range {
    min: string, 
    max: string, 
    step: string,
}

export interface DefinitionEntry {
    function: string,
    duration: string,
    resource: string,
}

export interface PropertyValueNumber {
    label: string,
    id: string,
    value: null | number,
    type: {type: 'int' | 'float'},
}

export interface PropertyValueString {
    label: string,
    id: string,
    value: null | string,
    type: {type: 'string'},
}

export interface PropertyValueEnum {
    label: string,
    id: string,
    value: null | string,
    type: {type: 'enum', values: string[]},
}

export type PropertyValue = PropertyValueNumber | PropertyValueString | PropertyValueEnum;

export interface PropertyValueGroup {
    label: string;
    id: string,
    propertyValues: PropertyValue[];
}

export interface Element {
    id: string;
    label?: string;
    propertyValueGroups: PropertyValueGroup[];
    type: 'ControlNode' | 'FunctionalChainInvolvementFunction' | 'SequenceLink';
    controlNodeKind?: 'AND' | 'OR' | 'ITERATE',
}

export interface FunctionalChain {
    id: string;
    name: string;
    elements: Element[];
}

export interface PVMT {
    name: string;
    functionalChains: FunctionalChain[];
}

export interface PropertyValueExtDurationViaResource extends Omit<Omit<PropertyValue, 'value'>, 'type'> {
    value: {[s: string]: {value: string, default: boolean}},
    type: {type: 'duration_via_resource'},
    propertyValueExtNumber: PropertyValueExtNumber, // Used when reverting back to PropertyValueExtNumber
}

export interface PropertyValueExtNumber extends Omit<PropertyValueNumber, 'value'> {
    value: null | Range | string,
}

export interface PropertyValueExtString extends Omit<PropertyValueString, 'value'> {
    value: string[],
}

export interface PropertyValueExtEnum extends Omit<PropertyValueEnum, 'value'> {
    value: string[],
}

export type PropertyValueExt = PropertyValueExtString | PropertyValueExtNumber | PropertyValueExtEnum | PropertyValueExtDurationViaResource;

export interface PropertyValueGroupExt extends Omit<PropertyValueGroup, 'propertyValues'> {
    propertyValues: PropertyValueExt[];
}

export interface ElementExt extends Omit<Element, 'propertyValueGroups'> {
    propertyValueGroups: PropertyValueGroupExt[];
}

export interface FunctionalChainExt extends Omit<FunctionalChain, "elements"> {
    elements: ElementExt[];
}

export interface PVMTExt extends Omit<PVMT, "functionalChains"> {
    functionalChains: FunctionalChainExt[];
}

export type PVMTExport = (PropertyValueExtString | PropertyValueExtNumber | PropertyValueExtEnum)[];

export function isPropertyValueExtString(value: PropertyValueExt): value is PropertyValueExtString {
    return value.type.type === 'string';
}

export function isPropertyValueExtDurationViaResource(value: PropertyValueExt): value is PropertyValueExtDurationViaResource {
    return value.type.type === 'duration_via_resource';
}

export function isPropertyValueExtEnum(value: PropertyValueExt): value is PropertyValueExtEnum {
    return value.type.type === 'enum';
}

export function isPropertyValueExtNumber(value: PropertyValueExt): value is PropertyValueExtNumber {
    return value.type.type === 'int' || value.type.type === 'float';
}

export function isPropertyValueNumber(value: PropertyValue): value is PropertyValueNumber {
    return value.type.type === 'int' || value.type.type === 'float';
}

export function isRange(value: Range | string): value is Range {
    return typeof value == 'object';
}