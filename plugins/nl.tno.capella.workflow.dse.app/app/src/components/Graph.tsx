/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React, {useEffect, useRef, useContext} from 'react';
import * as d3 from 'd3';
import {Context} from '../store';
import colors from '../colors';

export default function Graph() {
    const ref = useRef<HTMLDivElement>(null);
    const {state, dispatch} = useContext(Context);

    const elementsLookup = Object.fromEntries(state.pvmt.functionalChains.flatMap((fc) => fc.elements).map((e) => [e.id, e]));

    const filterIsElement = (_: any, i: number, e: any[]) => e[i].getAttribute('diagram:semantictargetid') in elementsLookup;
    const filterIsControlNode = (_: any, i: number, e: any[]) => elementsLookup[e[i].getAttribute('diagram:semantictargetid')].type === 'ControlNode';
    const filterStrokeNotNone = (_: any, i: number, e: any[]) => e[i].getAttribute('stroke') !== 'none';
    const filterMarkerTarget = (_: any, i: number, e: any[]) => e[i].getAttribute('marker-target');

    const id = 'capella-svg';

    useEffect(() => {
        ref.current.innerHTML = state.svg;
        const svg = ref.current.children[0] as SVGElement;
        svg.id = id;
        // @ts-ignore
        svg.style = "height: 100%; width: 100%";

        const g = d3.select(`#${id} g`);

        const handleZoom = (e: any) => g.attr('transform', e.transform);
        const zoom = d3.zoom().on('zoom', handleZoom);
        d3.select(`#${id}`).call(zoom);

        g.selectAll('line').filter(filterIsElement).attr('marker-target', true).each((l, i: number, d: SVGLineElement[]) => {
            const line = d[i];
            // Add helper line to make lines more easiliy selectable
            g.append('line')
                .attr('x1', line.getAttribute('x1'))
                .attr('x2', line.getAttribute('x2'))
                .attr('y1', line.getAttribute('y1'))
                .attr('y2', line.getAttribute('y2'))
                .attr('opacity', 0)
                .attr('stroke-width', 15)
                .attr(':diagram:semantictargetid', line.getAttribute('diagram:semantictargetid'))
        });

        // Add marker-target for control nodes
        g.selectAll('image').filter(filterIsElement).filter(filterIsControlNode).each((l, i: number, d: SVGImageElement[]) => {
            const image = d[i];
            g.append('circle')
                .attr('cy', Number(image.getAttribute('y')) + (Number(image.getAttribute('height')) / 2))
                .attr('cx', Number(image.getAttribute('x')) + (Number(image.getAttribute('width')) / 2))
                .attr('r', 12)
                .attr('fill', 'none')
                .attr('marker-target', true)
                .attr('stroke-width', 0)
                .attr(':diagram:semantictargetid', image.getAttribute('diagram:semantictargetid'))
        });

        // Add marker-target for functions
        g.selectAll('rect').filter(filterIsElement).filter(filterStrokeNotNone).attr('marker-target', true)

        g.selectAll('*')
            .filter(filterIsElement)
            .on('mouseover', (d) =>  dispatch({action: 'set_highlighted_element', id: d.srcElement.getAttribute('diagram:semantictargetid')}))
            .on('mouseout', () => dispatch({action: 'set_highlighted_element', id: null}))
            .on('click', (d) => dispatch({action: 'set_selected_element', id: d.srcElement.getAttribute('diagram:semantictargetid')}));
    }, [state.svg]);

    useEffect(() => {
        const getElementState = (element: any) => {
            const id = element.getAttribute('diagram:semantictargetid');
            if (id === state.selectedElement) return 'selected';
            else if (id === state.highlightedElement) return 'highlighted';
            else return 'none'
        };

        d3.select(`#${id} g`).selectAll('*').filter(filterMarkerTarget)
            .attr('stroke', (_, i: number, e: any[]) => {
                if (getElementState(e[i]) === 'selected') return colors.selected;
                else if (getElementState(e[i]) === 'highlighted') return colors.highlighted;
                else return 'black';
            })
            .attr('stroke-width', (_, i: number, e) => {
                return getElementState(e[i]) === 'none' ?
                    (e[i] instanceof SVGCircleElement ? 0 : 1) : 
                    5;
            });
    }, [state.selectedElement, state.highlightedElement])

    return (<div style={{height: '100%'}} ref={ref}/>)
}