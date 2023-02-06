#
# Copyright (c) 2022 ESI (TNO)
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

import sys, os, random, time, argparse, json

def transition_duration(transition):
    if 'Duration' in transition.props:
        return round(transition.props['Duration'] * 1000)
    return 0


def transition_or_weight(transition):
    weights = [i for i in transition.input_props if 'Weight' in i]
    return round(weights[0]['Weight']) if len(weights) > 0 else 1


def trace(net, events):
    result = "TU SECONDS\nO 0\nT\n"
    resources = list(set([t.props['ResourceID'] for t in net.transition() if t.name.startswith("FUNCS")]))
    for r in resources:
        result += f"R {resources.index(r)} 1 false;name={r}\n"

    cnt = 0
    for time, transition in events:
        if transition.name.startswith("FUNCS"):
            start = time / 1000
            end = (time + transition_duration(transition)) / 1000
            name = transition.name.split("_", 1)[1]
            level = transition.props['Level']
            resource = resources.index(transition.props['ResourceID'])
            levelNames = ','.join([f"level{i + 1}name={l}"for i, l in enumerate(transition.props['LevelNames'])])
            result += f"C {cnt} {start} {end} {resource} 1;name={name},level={level},{levelNames}\n"
            cnt += 1
    return result

def simulate(net, initial_marking, verbose):
    clock = 0 # 1 is 1 ms
    events = []
    blocked = {}
    while True:
        transitions = [t for t in net.transition() if len(t.modes()) > 0]
        if len(transitions) == 0: break
        transitions = [t for t in transitions if not t.name in blocked]
        if len(transitions) == 0:
            # Everything is blocked, forward the clock
            forward_clock = min(blocked.values())
            for e in list(blocked.keys()):
                blocked[e] -= forward_clock
                if blocked[e] == 0:
                    del blocked[e]
            clock += forward_clock
        else:
            transition = random.choice(transitions)
            # If transition starts from ORSTART, also consider the other transitions based on weight
            orstart = [p[0] for p in transition.input() if p[0].name.startswith("ORS")]
            if len(orstart) > 0:
                orstart = orstart[0]
                transitions_weighted = []
                for t in [t for t in transitions if orstart in t._input]:
                    weight = transition_or_weight(t)
                    transitions_weighted.extend([t] * weight)
                transition = random.choice(transitions_weighted)

            mode = random.choice(transition.modes())
            transition.fire(mode)
            events.append((clock, transition))
            if transition.name.startswith("FUNC"):
                name = transition.name.split('_', 1)[1]
                postfix = '->' if transition.name.startswith("FUNCS") else '<-'
                if verbose:
                    print(f"{(clock/1000):.3f}s {postfix} {'  ' * transition.branch_depth}{name}")
            if transition.name.startswith("FUNCS"):
                duration = transition_duration(transition)
                if duration > 0:
                    blocked[f"FUNCE{transition.name[5:]}"] = duration

    net.set_marking(initial_marking)
    return events


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('--loop', action='store_true')
    parser.add_argument('--verbose', action='store_true')
    args = parser.parse_args()

    sys.path.append(os.path.dirname(__file__))
    import net
    initial_marking = net.n.get_marking()

    while True:
        start = time.time()
        events = simulate(net.n, initial_marking, args.verbose)
        end = time.time()

        trace_output = trace(net.n, events)
        trace_file = os.path.join(os.path.dirname(net.__file__), "trace.etf")
        with open(trace_file, 'w') as f: f.write(trace_output)
        if args.verbose: print(f"Wrote TRACE to '{trace_file}'")

        result_output = json.dumps({'time': events[-1][0] / 1000})
        result_file = os.path.join(os.path.dirname(net.__file__), "result.json")
        with open(result_file, 'w') as f: f.write(result_output)
        if args.verbose: print(f"Wrote results to '{result_file}'")

        if args.loop:
            input(f"Simulation finished, took {end - start:.3f}s, do you want to run again? (press enter)\n")
        else:
            break
