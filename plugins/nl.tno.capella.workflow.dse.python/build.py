#
# Copyright (c) 2022 ESI (TNO)
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

import os, sys, glob, subprocess

PROJECT_DIR = os.path.abspath(os.path.dirname(__file__))
PYTHON_DIR = os.path.abspath(os.path.dirname(sys.executable))

print("Installing dependencies")
print(os.path.join(PROJECT_DIR, 'dependencies', '*', '*', 'setup.py'))
for setup in glob.glob(os.path.join(PROJECT_DIR, 'dependencies', '*', '*', 'setup.py')):
    print(setup)
    subprocess.check_call([sys.executable, setup, 'install'], cwd=os.path.dirname(setup))

print("Modify pth")
pth_file = glob.glob(os.path.join(PYTHON_DIR, "python*._pth"))[0]
with open(pth_file, 'r') as f: pth_contents = f.read()
pth_contents = pth_contents.replace('#import site', 'import site')
with open(pth_file, 'w') as f: f.write(pth_contents)

print('Done!')