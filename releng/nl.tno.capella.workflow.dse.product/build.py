#
# Copyright (c) 2022 ESI (TNO)
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

import os, subprocess, glob, zipfile, sys, shutil

REPOSITORIES = [
    'http://www.es.ele.tue.nl/rotalumis/repository/release',
    'https://download.eclipse.org/trace4cps/v0.1/update-site',
    'https://download.eclipse.org/modeling/tmf/xtext/updates/releases/2.22.0/',
    'https://eclipse.github.io/poosl/release/1.0.2',
    *['jar:file:/' + os.path.abspath(z).replace('\\', '/') + '!/' for z in glob.glob('p2/*.zip')]
]

FEATURES = [
    'org.eclipse.trace4cps.feature.feature.group',
    'org.eclipse.poosl.feature.feature.group',
    'com.thalesgroup.vpd.property.feature.feature.group'
]

COMMAND = [
    'capellac', '-purgeHistory', '-application', 'org.eclipse.equinox.p2.director', '-noSplash',
    '-repository', ','.join(REPOSITORIES),
    '-installIUs', ','.join(FEATURES),
]

def zipdir(path, ziph):
    for root, _, files in os.walk(path):
        for file in files:
            ziph.write(os.path.join(root, file), os.path.relpath(os.path.join(root, file), os.path.join(path)))

DIST_PATH = 'dist'
shutil.rmtree(DIST_PATH, ignore_errors=True)

for arch in os.listdir('capella-product'):
    print(f"Building {arch} product")
    path = os.path.join('capella-product', arch, 'capella')

    # Install dependencies
    print('Installing dependencies')
    if subprocess.call(COMMAND, cwd=path, shell=True) != 0:
        raise Exception("Dependencies install failed")

    # Install plugins
    plugins = glob.glob('../../plugins/*/target/nl.tno.capella.workflow.dse*.jar')
    for plugin in plugins:
        plugin_name = os.path.splitext(os.path.basename(plugin))[0]
        print(f"Installing {plugin_name}")
        with zipfile.ZipFile(plugin, 'r') as zip_ref:
            zip_ref.extractall("\\\\?\\" + os.path.abspath(os.path.join(path, 'plugins', plugin_name)))

    # Update eclipse.product (otherwise .poosl files cannot be executed)
    print('Updating config.ini')
    config_ini = os.path.join(path, 'configuration', 'config.ini')
    with open(config_ini, 'r') as f: config_ini_content = f.read()
    config_ini_content = config_ini_content.replace(
        "eclipse.product=org.polarsys.capella.rcp.product", 
        "eclipse.product=org.eclipse.platform.ide"
    )
    with open(config_ini, 'w') as f: f.write(config_ini_content)

    # Write version.txt
    version = sys.argv[1]
    with open(os.path.join(path, 'version.txt'), 'w') as f:
        f.write(version)
    version_txt = os.path.join(path, 'configuration', 'config.ini')

    # Zip
    os.makedirs(DIST_PATH, exist_ok=True)
    zip_file = os.path.join(DIST_PATH, f'capella-workflow-dse-{arch}-{version}.zip')
    print(f"Creating '{zip_file}'")
    with zipfile.ZipFile(zip_file, 'w', zipfile.ZIP_DEFLATED) as zipf:
        # Long path prefix required for Windows
        zipdir("\\\\?\\" + os.path.abspath(path), zipf)

print('Done')