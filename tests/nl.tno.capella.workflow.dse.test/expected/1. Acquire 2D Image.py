import snakes.plugins
snakes.plugins.load('gv', 'snakes.nets', 'nets')
from nets import *

n = PetriNet('N')

# Places
p = Place('RES1_42', [1])
p.props = {}
n.add_place(p)
p = Place('ITS6')
p.props = {
	'Description': '',
	'Repetitions': 10,
}
n.add_place(p)
p = Place('ITS7')
p.props = {
	'Description': '',
	'Repetitions': 10,
}
n.add_place(p)
p = Place('ITG10')
p.props = {
	'Description': '',
	'Repetitions': 10,
}
n.add_place(p)
p = Place('ITE11')
p.props = {}
n.add_place(p)
p = Place('ITE12')
p.props = {}
n.add_place(p)
p = Place('ITL14', [1])
p.props = {}
n.add_place(p)
p = Place('N15')
p.props = {}
n.add_place(p)
p = Place('START16', [1])
p.props = {}
n.add_place(p)
p = Place('END17')
p.props = {}
n.add_place(p)
p = Place('N18')
p.props = {}
n.add_place(p)
p = Place('N19')
p.props = {}
n.add_place(p)
p = Place('N20')
p.props = {}
n.add_place(p)
p = Place('N21')
p.props = {}
n.add_place(p)

# Transitions
t = Transition('FUNCS2_1.2. Acquire 2D Image')
t.props = {
	'Duration': 5.000000,
	'Level': 1,
	'Description': '',
	'ResourceID': '42',
	'LevelNames': ['1.2. Acquire 2D Image'],
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE2_1.2. Acquire 2D Image')
t.props = {
	'Duration': 5.000000,
	'Level': 1,
	'Description': '',
	'ResourceID': '42',
	'LevelNames': ['1.2. Acquire 2D Image'],
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS3_1.1. Move to Next Position')
t.props = {
	'Duration': 1.000000,
	'Level': 1,
	'ResourceID': 't3Unknown',
	'LevelNames': ['1.1. Move to Next Position'],
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCE3_1.1. Move to Next Position')
t.props = {
	'Duration': 1.000000,
	'Level': 1,
	'ResourceID': 't3Unknown',
	'LevelNames': ['1.1. Move to Next Position'],
}
t.branch_depth = 1
n.add_transition(t)
t = Transition('FUNCS4_1.0 Prepare for 2D acquisitions')
t.props = {
	'Duration': 0.100000,
	'Level': 1,
	'Description': '',
	'ResourceID': 't4Unknown',
	'LevelNames': ['1.0 Prepare for 2D acquisitions'],
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCE4_1.0 Prepare for 2D acquisitions')
t.props = {
	'Duration': 0.100000,
	'Level': 1,
	'Description': '',
	'ResourceID': 't4Unknown',
	'LevelNames': ['1.0 Prepare for 2D acquisitions'],
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCS5_1.3 Finalize 2D acquisitions')
t.props = {
	'Duration': 0.100000,
	'Level': 1,
	'Description': '',
	'ResourceID': 't5Unknown',
	'LevelNames': ['1.3 Finalize 2D acquisitions'],
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('FUNCE5_1.3 Finalize 2D acquisitions')
t.props = {
	'Duration': 0.100000,
	'Level': 1,
	'Description': '',
	'ResourceID': 't5Unknown',
	'LevelNames': ['1.3 Finalize 2D acquisitions'],
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITS8')
t.props = {
	'ResourceID': 't8Unknown',
	'LevelNames': ['x'],
	'Level': 1,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITG9')
t.props = {
	'ResourceID': 't9Unknown',
	'LevelNames': ['x'],
	'Level': 1,
}
t.branch_depth = 0
n.add_transition(t)
t = Transition('ITE13')
t.props = {
	'ResourceID': 't13Unknown',
	'LevelNames': ['x'],
	'Level': 1,
}
t.branch_depth = 1
n.add_transition(t)
for t in n.transition():
    t.input_props = []
    t.output_props = []

# Inputs
n.add_input('RES1_42', 'FUNCS2_1.2. Acquire 2D Image', Value(1))
n.add_input('ITS6', 'ITS8', Value(1))
n.add_input('ITG10', 'ITG9', Value(1))
n.add_input('ITE11', 'ITE13', Value(1))
n.add_input('ITS7', 'FUNCS3_1.1. Move to Next Position', Value(1))
n.add_input('ITE12', 'FUNCS5_1.3 Finalize 2D acquisitions', MultiArc([Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1)]))
n.add_input('ITL14', 'ITS8', Value(1))
n.add_input('N15', 'FUNCS2_1.2. Acquire 2D Image', Value(1))
n.add_input('START16', 'FUNCS4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_input('N18', 'FUNCE4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_input('N19', 'FUNCE5_1.3 Finalize 2D acquisitions', Value(1))
n.add_input('N20', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_input('N21', 'FUNCE3_1.1. Move to Next Position', Value(1))

# Outputs
n.add_output('RES1_42', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_output('ITS7', 'ITS8', Value(1))
n.add_output('ITS6', 'ITG9', MultiArc([Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1),Value(1)]))
n.add_output('ITE12', 'ITE13', Value(1))
n.add_output('ITE11', 'FUNCE2_1.2. Acquire 2D Image', Value(1))
n.add_output('ITL14', 'ITE13', Value(1))
n.add_output('ITG10', 'FUNCE4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_output('N15', 'FUNCE3_1.1. Move to Next Position', Value(1))
n.add_output('END17', 'FUNCE5_1.3 Finalize 2D acquisitions', Value(1))
n.add_output('N18', 'FUNCS4_1.0 Prepare for 2D acquisitions', Value(1))
n.add_output('N19', 'FUNCS5_1.3 Finalize 2D acquisitions', Value(1))
n.add_output('N20', 'FUNCS2_1.2. Acquire 2D Image', Value(1))
n.add_output('N21', 'FUNCS3_1.1. Move to Next Position', Value(1))