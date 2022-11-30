import snakes.plugins
snakes.plugins.load('gv', 'snakes.nets', 'nets')
from nets import *

n = PetriNet('N')

# Places
p = Place('RES1_1', [1])
p.props = {}
n.add_place(p)
p = Place('ITS6')
p.props = {
	'Repetitions': 2,
}
n.add_place(p)
p = Place('ITS7')
p.props = {
	'Repetitions': 2,
}
n.add_place(p)
p = Place('ITG10')
p.props = {
	'Repetitions': 2,
}
n.add_place(p)
p = Place('ITE11')
p.props = {}
n.add_place(p)
p = Place('ITE12')
p.props = {}
n.add_place(p)
p = Place('ORS14')
p.props = {}
n.add_place(p)
p = Place('ORE15')
p.props = {}
n.add_place(p)
p = Place('ITL16', [1])
p.props = {}
n.add_place(p)
p = Place('START19', [1])
p.props = {}
n.add_place(p)
p = Place('END20')
p.props = {}
n.add_place(p)
p = Place('N21')
p.props = {}
n.add_place(p)
p = Place('N22')
p.props = {}
n.add_place(p)
p = Place('N23')
p.props = {}
n.add_place(p)
p = Place('N24')
p.props = {}
n.add_place(p)

# Transitions
t = Transition('FUNCS2_2.2. Process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '1',
	'Duration': 8.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCE2_2.2. Process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '1',
	'Duration': 8.000000,
}
t.branch_depth = 2
n.add_transition(t)
t = Transition('FUNCS3_2.1. Pre-process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 1.000000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE3_2.1. Pre-process 2D Image')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 1.000000,
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS4_2.0 Prepare for 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCE4_2.0 Prepare for 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCS5_2.3. Finalize 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCE5_2.3. Finalize 2D image processing')
t.props = {
	'Description': '',
	'ResourceID': '',
	'Duration': 0.100000,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITS8')
t.props = {}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITG9')
t.props = {}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITE13')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('N17')
t.props = {}
t.branch_depth = 1
n.add_transition(t)
t = Transition('OR18')
t.props = {}
t.branch_depth = 2
n.add_transition(t)
for t in n.transition():
    t.input_props = []
    t.output_props = []

# Inputs
n.add_input('RES1_1', 'FUNCS2_2.2. Process 2D Image', Value(1))
n.add_input('ITS6', 'ITS8', Value(1))
n.add_input('ITG10', 'ITG9', Value(1))
n.add_input('ITE11', 'ITE13', Value(1))
n.add_input('ITS7', 'FUNCS3_2.1. Pre-process 2D Image', Value(1))
n.add_input('ITE12', 'FUNCS5_2.3. Finalize 2D image processing', Value(1))
n.add_input('ITL16', 'ITS8', Value(1))
n.add_input('ORS14', 'FUNCS2_2.2. Process 2D Image', Value(1))
n.add_input('ORE15', 'N17', Value(1))
n.add_input('ORS14', 'OR18', Value(1))
n.transition('OR18').input_props.append({
	'Weight': 1.000000,
})
n.add_input('START19', 'FUNCS4_2.0 Prepare for 2D image processing', Value(1))
n.add_input('N21', 'FUNCE3_2.1. Pre-process 2D Image', Value(1))
n.add_input('N22', 'FUNCE5_2.3. Finalize 2D image processing', Value(1))
n.add_input('N23', 'FUNCE2_2.2. Process 2D Image', Value(1))
n.add_input('N24', 'FUNCE4_2.0 Prepare for 2D image processing', Value(1))

# Outputs
n.add_output('RES1_1', 'FUNCE2_2.2. Process 2D Image', Value(1))
n.add_output('ITS7', 'ITS8', Value(1))
n.add_output('ITS6', 'ITG9', MultiArc([Value(1),Value(1)]))
n.add_output('ITE12', 'ITE13', Value(1))
n.add_output('ITG10', 'FUNCE4_2.0 Prepare for 2D image processing', Value(1))
n.add_output('ITL16', 'ITE13', Value(1))
n.add_output('ORS14', 'FUNCE3_2.1. Pre-process 2D Image', Value(1))
n.add_output('ORE15', 'FUNCE2_2.2. Process 2D Image', Value(1))
n.add_output('ITE11', 'N17', Value(1))
n.add_output('ORE15', 'OR18', Value(1))
n.transition('OR18').output_props.append({
	'Weight': 1.000000,
})
n.add_output('END20', 'FUNCE5_2.3. Finalize 2D image processing', Value(1))
n.add_output('N21', 'FUNCS3_2.1. Pre-process 2D Image', Value(1))
n.add_output('N22', 'FUNCS5_2.3. Finalize 2D image processing', Value(1))
n.add_output('N23', 'FUNCS2_2.2. Process 2D Image', Value(1))
n.add_output('N24', 'FUNCS4_2.0 Prepare for 2D image processing', Value(1))