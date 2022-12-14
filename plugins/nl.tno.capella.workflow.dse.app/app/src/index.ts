/**
 * Copyright (c) 2022 ESI (TNO)
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
import { app, BrowserWindow, ipcMain, dialog } from 'electron';
import fs from 'fs';
import path from 'path';
import process from 'process';
import {DefinitionEntry, PVMT, Run} from './types';

// This allows TypeScript to pick up the magic constants that's auto-generated by Forge's Webpack
// plugin that tells the Electron app where to look for the Webpack-bundled app code (depending on
// whether you're running in development or production).
declare const MAIN_WINDOW_WEBPACK_ENTRY: string;
declare const MAIN_WINDOW_PRELOAD_WEBPACK_ENTRY: string;

// Handle creating/removing shortcuts on Windows when installing/uninstalling.
if (require('electron-squirrel-startup')) {
  // eslint-disable-line global-require
  app.quit();
}

const createWindow = (): void => {
  // Create the browser window.
  const mainWindow = new BrowserWindow({
    height: 600,
    width: 800,
    webPreferences: {
      preload: MAIN_WINDOW_PRELOAD_WEBPACK_ENTRY,
      nodeIntegration: true,
      contextIsolation: false,
    },
  });
  mainWindow.maximize();

  // and load the index.html of the app.
  mainWindow.loadURL(MAIN_WINDOW_WEBPACK_ENTRY);
};

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.on('ready', createWindow);

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  // On OS X it's common to re-create a window in the app when the
  // dock icon is clicked and there are no other windows open.
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});

const root = process.argv[process.argv.length - 1];

ipcMain.on('get-data', (event) => {
  const svgFile = path.join(root, 'gen', 'dse', 'diagram.svg');
  const dseFile = path.join(root, 'gen', 'dse', 'dse.json');
  const definitionsFile = path.join(root, 'DSE-definitions.json')
  const svg = fs.readFileSync(svgFile).toString();
  const dse = JSON.parse(fs.readFileSync(dseFile).toString());
  const definitions = fs.existsSync(definitionsFile) ? JSON.parse(fs.readFileSync(definitionsFile).toString()) : [];
  event.returnValue = {svg, pvmt: dse.pvmt, configurationItems: dse.configurationItems, definitions};
});

const sleep = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

let devRunCounter = 0;

ipcMain.handle('list-runs', async (): Promise<string[]> => {
  if (!app.isPackaged) {
    return Array.from({length: devRunCounter}, (_, i) => "run-" + (i + 1));
  } else {
    return fs.readdirSync(path.join(root, 'gen', 'dse')).filter((d) => d.startsWith("run"));
  }
});

ipcMain.handle('save-definitions', async (event, definitions: DefinitionEntry[]) => {
  console.log(JSON.stringify({action: 'save-definitions', definitions}));
});

ipcMain.handle('clear-runs', async () => {
  console.log(JSON.stringify({action: 'clear-runs'}));
});

let cancelRun: () => void = null
ipcMain.handle('cancel-run', async () => {
  console.log(JSON.stringify({action: 'cancel-run'}));
  cancelRun && cancelRun();
});

ipcMain.handle('save-file', async (event, title: string, filters: {name: string, extensions: string[]}[], filename: string, 
    data: string) => {
  const file = await dialog.showSaveDialog({title, filters, defaultPath: path.join(root, filename)});
  if (!file.canceled) {
    fs.writeFileSync(file.filePath, data);
  }
});

ipcMain.handle('load-file', async (event, title: string, filters: {name: string, extensions: string[]}[]) => {
  const file = await dialog.showOpenDialog({title, filters, defaultPath: root, properties: ['openFile']});
  if (!file.canceled) {
    return fs.readFileSync(file.filePaths[0], 'utf-8')
  }
});

ipcMain.handle('run', async (event, name: string, pvmt: PVMT): Promise<Run> => {
  console.log(JSON.stringify({action: 'run', pvmt, name}));

  return new Promise(async (resolve, reject) => {
    let rejected = false;
    cancelRun = () => {
      rejected = true;
      reject(new Error('run-canceled'));
    }

    if (!app.isPackaged) {
      devRunCounter += 1;
      await sleep(500);
      resolve({name, time: (Math.random() * 5) + 1, pvmt});
    } else {
      while (!rejected) {
        await sleep(200);
        const rotalumisTxt = path.join(root, 'gen', 'dse', name, 'rotalumis.txt');
        if (fs.existsSync(rotalumisTxt)) {
          const content = fs.readFileSync(rotalumisTxt, 'utf8').toString();
          const time = Number(content.match('Simulated time: +(\\d+)')[1]);
          resolve({name, time, pvmt});
          break;
        }
      }
    }
  });
});