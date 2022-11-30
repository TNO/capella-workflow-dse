/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import React from 'react';
import Graph from './Graph';
import Header from './Header';
import PVMTEditor from './PVMTEditor';
import RunsDialog from './RunsDialog';
import { Allotment } from 'allotment';

export default function App() {
  return (
    <div style={{height: '100vh', width: '100vw'}}>
      <Allotment minSize={100} defaultSizes={[2, 5]}>
          <PVMTEditor/>
          <div style={{height: '100%', width: '100%'}}>
            <Header/>
            <Graph/>
          </div>
        </Allotment>
        <RunsDialog/>
    </div>
  );
}
