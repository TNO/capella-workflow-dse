import snakes.plugins
snakes.plugins.load('gv', 'snakes.nets', 'nets')
from nets import *

n = PetriNet('N')

# Places
p = Place('RES1_42', [1])
p.props = {}
n.add_place(p)
p = Place('RES6_1', [1])
p.props = {}
n.add_place(p)
p = Place('ITS13')
p.props = {
	'Description': '',
	'Repetitions': 10,
}
n.add_place(p)
p = Place('ITS14')
p.props = {
	'Description': '',
	'Repetitions': 10,
}
n.add_place(p)
p = Place('ITG17')
p.props = {
	'Description': '',
	'Repetitions': 10,
}
n.add_place(p)
p = Place('ITE18')
p.props = {}
n.add_place(p)
p = Place('ITE19')
p.props = {}
n.add_place(p)
p = Place('ITS21')
p.props = {
	'Repetitions': 2,
}
n.add_place(p)
p = Place('ITS22')
p.props = {
	'Repetitions': 2,
}
n.add_place(p)
p = Place('ITG25')
p.props = {
	'Repetitions': 2,
}
n.add_place(p)
p = Place('ITE26')
p.props = {}
n.add_place(p)
p = Place('ITE27')
p.props = {}
n.add_place(p)
p = Place('ORS29')
p.props = {}
n.add_place(p)
p = Place('ORE30')
p.props = {}
n.add_place(p)
p = Place('ANDS31')
p.props = {}
n.add_place(p)
p = Place('ANDE33')
p.props = {}
n.add_place(p)
p = Place('ITL35', [1])
p.props = {}
n.add_place(p)
p = Place('N36')
p.props = {}
n.add_place(p)
p = Place('ITL37', [1])
p.props = {}
n.add_place(p)
p = Place('ANDS40')
p.props = {}
n.add_place(p)
p = Place('ANDS41')
p.props = {}
n.add_place(p)
p = Place('N42')
p.props = {}
n.add_place(p)
p = Place('ANDE43')
p.props = {}
n.add_place(p)
p = Place('START44', [1])
p.props = {}
n.add_place(p)
p = Place('END45')
p.props = {}
n.add_place(p)
p = Place('N46')
p.props = {}
n.add_place(p)
p = Place('N47')
p.props = {}
n.add_place(p)
p = Place('N48')
p.props = {}
n.add_place(p)
p = Place('N49')
p.props = {}
n.add_place(p)
p = Place('N50')
p.props = {}
n.add_place(p)
p = Place('N51')
p.props = {}
n.add_place(p)
p = Place('N52')
p.props = {}
n.add_place(p)
p = Place('N53')
p.props = {}
n.add_place(p)
p = Place('N54')
p.props = {}
n.add_place(p)
p = Place('N55')
p.props = {}
n.add_place(p)

# Transitions
t = Transition('FUNCS2_1.2. Acquire 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '42',
	'Duration': 5.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCE2_1.2. Acquire 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '42',
	'Duration': 5.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCS3_1.1. Move to Next Position')
t.props = {
	'Duration': 1.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCE3_1.1. Move to Next Position')
t.props = {
	'Duration': 1.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCS4_1.0 Prepare for 2D acquisitions')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE4_1.0 Prepare for 2D acquisitions')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS5_1.3 Finalize 2D acquisitions')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE5_1.3 Finalize 2D acquisitions')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS7_2.2. Process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '1',
	'Duration': 8.000000,
}
t.branch_depth = 3
n.add_transition(t)
t = Transition('FUNCE7_2.2. Process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '1',
	'Duration': 8.000000,
}
t.branch_depth = 3
n.add_transition(t)
t = Transition('FUNCS8_2.1. Pre-process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 1.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCE8_2.1. Pre-process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 1.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCS9_2.0 Prepare for 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE9_2.0 Prepare for 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS10_2.3. Finalize 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE10_2.3. Finalize 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS11_3. Create 3D Image from 2D Images')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 5.000000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCE11_3. Create 3D Image from 2D Images')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 5.000000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCS12_0. Initiate 3D reconstruction')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.050000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCE12_0. Initiate 3D reconstruction')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.050000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITS15')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('ITG16')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('ITE20')
t.props = {}
t.branch_depth = 2
n.add_transition(t)
t = Transition('ITS23')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('ITG24')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('ITE28')
t.props = {}
t.branch_depth = 2
n.add_transition(t)
t = Transition('AND32')
t.props = {}
t.branch_depth = 0
n.add_transition(t)
t = Transition('AND34')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('N38')
t.props = {}
t.branch_depth = 2
n.add_transition(t)
t = Transition('OR39')
t.props = {}
t.branch_depth = 3
n.add_transition(t)
for t in n.transition():
    t.input_props = []
    t.output_props = []

# Inputs
n.add_input('RES1_42', 'FUNCS2_1.2. Acquire 2D Image', Value(1))
n.add_input('RES6_1', 'FUNCS7_2.2. Process 2D Image', Value(1))
n.add_input('ITS13', 'ITS15', Value(1))
n.add_input('ITG17', 'ITG16', Value(1))
n.add_input('ITE18', 'ITE20', Value(1))
n.add_input('ITS21', 'ITS23', Value(1))
n.add_input('ITG25', 'ITG24', Value(1))
n.add_input('ITE26', 'ITE28', Value(1))
n.add_input('ANDS31', 'AND32', Value(1))
n.add_input('ANDE33', 'AND34', MultiArc([Value(1),Value(1)]))
n.add_input('ITS14', 'FUNCS3_1.1. Move to Next Position', Value(1))
n.add_input('ITE19', 'FUNCS5_1.3 Finalize 2D acquisitions', Value(1))
n.add_input('ITL35', 'ITS15', Value(1))
n.add_input('N36', 'FUNCS2_1.2. Acquire 2D Image', Value(1))
n.add_input('ITS22', 'FUNCS8_2.1. Pre-process 2D Image', Value(1))
n.add_input('ITE27', 'FUNCS10_2.3. Finalize 2D image processing', Value(1))
n.add_input('ITL37', 'ITS23', Value(1))
n.add_input('ORS29', 'FUNCS7_2.2. Process 2D Image', Value(1))
n.add_input('ORE30', 'N38', Value(1))
n.add_input('ORS29', 'OR39', Value(1))
n.transition('OR39').input_props.append({
	'Weight': 1.000000,
})
n.add_input('ANDS40', 'FUNCS4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_input('ANDS41', 'FUNCS9_2.0 Prepare for 2D image processing', Value(1))
n.add_input('N42', 'FUNCS8_2.1. Pre-process 2D Image', Value(1))
n.add_input('ANDE43', 'FUNCS11_3. Create 3D Image from 2D Images', Value(1))
n.add_input('START44', 'FUNCS12_0. Initiate 3D reconstruction', Value(1))
n.add_input('N46', 'FUNCE8_2.1. Pre-process 2D Image', Value(1))
n.add_input('N47', 'FUNCE10_2.3. Finalize 2D image processing', Value(1))
n.add_input('N48', 'FUNCE7_2.2. Process 2D Image', Value(1))
n.add_input('N49', 'FUNCE9_2.0 Prepare for 2D image processing', Value(1))
n.add_input('N50', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_input('N51', 'FUNCE11_3. Create 3D Image from 2D Images', Value(1))
n.add_input('N52', 'FUNCE3_1.1. Move to Next Position', Value(1))
n.add_input('N53', 'FUNCE4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_input('N54', 'FUNCE12_0. Initiate 3D reconstruction', Value(1))
n.add_input('N55', 'FUNCE5_1.3 Finalize 2D acquisitions', Value(1))

# Outputs
n.add_output('RES1_42', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_output('RES6_1', 'FUNCE7_2.2. Process 2D Image', Value(1))
n.add_output('ITS14', 'ITS15', Value(1))
n.add_output('ITS13', 'ITG16', MultiArc([Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1)]))
n.add_output('ITE19', 'ITE20', Value(1))
n.add_output('ITS22', 'ITS23', Value(1))
n.add_output('ITS21', 'ITG24', MultiArc([Value(1),Value(1)]))
n.add_output('ITE27', 'ITE28', Value(1))
n.add_output('ITE18', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_output('ITL35', 'ITE20', Value(1))
n.add_output('ITG17', 'FUNCE4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_output('N36', 'FUNCE3_1.1. Move to Next Position', Value(1))
n.add_output('ITG25', 'FUNCE9_2.0 Prepare for 2D image processing', Value(1))
n.add_output('ITL37', 'ITE28', Value(1))
n.add_output('ORS29', 'FUNCE8_2.1. Pre-process 2D Image', Value(1))
n.add_output('ORE30', 'FUNCE7_2.2. Process 2D Image', Value(1))
n.add_output('ITE26', 'N38', Value(1))
n.add_output('ORE30', 'OR39', Value(1))
n.transition('OR39').output_props.append({
	'Weight': 1.000000,
})
n.add_output('ANDS40', 'AND32', Value(1))
n.add_output('ANDS41', 'AND32', Value(1))
n.add_output('N42', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_output('ANDE33', 'FUNCE5_1.3 Finalize 2D acquisitions', Value(1))
n.add_output('ANDE33', 'FUNCE10_2.3. Finalize 2D image processing', Value(1))
n.add_output('ANDE43', 'AND34', Value(1))
n.add_output('ANDS31', 'FUNCE12_0. Initiate 3D reconstruction', Value(1))
n.add_output('END45', 'FUNCE11_3. Create 3D Image from 2D Images', Value(1))
n.add_output('N46', 'FUNCS8_2.1. Pre-process 2D Image', Value(1))
n.add_output('N47', 'FUNCS10_2.3. Finalize 2D image processing', Value(1))
n.add_output('N48', 'FUNCS7_2.2. Process 2D Image', Value(1))
n.add_output('N49', 'FUNCS9_2.0 Prepare for 2D image processing', Value(1))
n.add_output('N50', 'FUNCS2_1.2. Acquire 2D Image', Value(1))
n.add_output('N51', 'FUNCS11_3. Create 3D Image from 2D Images', Value(1))
n.add_output('N52', 'FUNCS3_1.1. Move to Next Position', Value(1))
n.add_output('N53', 'FUNCS4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_output('N54', 'FUNCS12_0. Initiate 3D reconstruction', Value(1))
n.add_output('N55', 'FUNCS5_1.3 Finalize 2D acquisitions', Value(1))