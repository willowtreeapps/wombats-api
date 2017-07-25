from copy import deepcopy
turn_directions = ['right', 'left', 'about-face']
smoke_directions = ['forward', 'backward', 'left', 'right', 'drop']
game_parameters = {
    # HP Modifiers
    'collision-hp-damage': 10,
    'food-hp-bonus': 5,
    'poison-hp-damage': 10,
    # Score Modifiers
    'food-score-bonus': 10,
    'wombat-hit-bonus': 10,
    'zakano-hit-bonus': 8,
    'steel-barrier-hit-bonus': 2,
    'wood-barrier-hit-bonus': 2,
    'wombat-destroyed-bonus': 25,
    'zakano-destroyed-bonus': 15,
    'wood-barrier-destroyed-bonus': 3,
    'steel-barrier-destroyed-bonus': 25,
    # In game parameters
    'shot-distance': 5
}

def get_arena_size(state):
    '''Fetches the size of one side of the arena from the state'''
    return state['global-dimensions'][0]

def add_locs(arena):
    '''Add local x and y coordinates to the arena matrix'''
    arena = deepcopy(arena)
    for x in range(len(arena)):
        for y in range(len(arena)):
            arena[y][x]['x'] = x
            arena[y][x]['y'] = y
    return arena

def filter_arena(arena, filters=None):
    '''Filter the arena to return only nodes that contain one of the supplied types'''
    if filters is None:
        return [tile for row in arena for tile in row]
    else:
        return [tile for row in arena for tile in row if tile['contents']['type'] in filters]

def build_initial_global_state(global_size):
    '''Constructs an intitial global state populated by fog'''
    return [[{'contents': {'type': 'fog'}} for _ in range(global_size)] for _ in range(global_size)]

def add_to_state(arena, elem):
    '''Update the global state with the given element and position'''
    # Modifies the arena object passed in and returns it
    arena[elem['y']][elem['x']] = elem
    return arena

def merge_global_state(global_arena, local_state, global_size):
    '''Add local state vision to global saved state.
    Position is that of the player which corresponds to (3,3) in local matrix'''
    x_offset = (local_state['global-coords'][0] - 3) % global_size
    y_offset = (local_state['global-coords'][1] - 3) % global_size
    local_tiles = filter_arena(add_locs(local_state['arena']),
                               ['food', 'poison', 'open',
                                'wood-barrier', 'steel-barrier'])
    # We want to add that the tile the player currently occupies is open
    wombat = {'contents': {'type': 'open'},
              'x': local_state['global-coords'][0],
              'y': local_state['global-coords'][1]
              }
    for tile in local_tiles:
        tile['x'] = (tile['x'] + x_offset) % global_size
        tile['y'] = (tile['y'] + y_offset) % global_size
        global_arena = add_to_state(global_arena, tile)

    return add_to_state(global_arena, wombat)


def get_global_state(state, path):
    '''Tries to fetch the global arena from the saved state or constructs a new one
    path is the location of the saved arena from the state object'''
    temp = state
    for level in path:
        temp = temp.get(level, {})
        if temp is None:
            # temp[level] might exist in dictionary, but be None
            temp = {}
            break
    if temp == {}:
        return build_initial_global_state(state['global-dimensions'][0])
    else:
        return temp

def get_direction(arena):
    '''Gets the current direction of your wombat from the 2d arena array'''
    return arena[3][3]['contents']['orientation']

def is_facing(dir, target, arena_half, wombat={'x': 3, 'y': 3}):
    '''
    Returns true if a move forward will bring you closer to the target location
    If no self coordinated are provided, use distance from (x, y)
    '''
    if dir == 'n':
        return ((target['y'] != wombat['y']) and
                (arena_half >= ((wombat['y'] - target['y']) % (arena_half * 2))))
    elif dir == 'e':
        return ((target['x'] != wombat['x']) and
                (arena_half >= ((target['x'] - wombat['x']) % (arena_half * 2))))
    elif dir == 's':
        return ((target['y'] != wombat['y']) and
                (arena_half >= ((target['y'] - wombat['y']) % (arena_half * 2))))
    elif dir == 'w':
        return ((target['x'] != wombat['x']) and
                (arena_half >= ((wombat['x'] - target['x']) % (arena_half * 2))))
    else:
        # This shouldn't happen
        return False

def distance_to_tile(dir, node, arena_size, wombat={'x': 3, 'y': 3}):
    facing = 0 if is_facing(dir, node, arena_size / 2, wombat) else 1
    x_dist = min(abs(node['x'] - wombat['x']), arena_size + 1 +
                 min(node['x'], wombat['x']) - max(node['x'], wombat['x']))
    y_dist = min(abs(node['y'] - wombat['y']), arena_size + 1 +
                 min(node['y'], wombat['y']) - max(node['y'], wombat['y']))
    return x_dist + y_dist + facing

def turn_to_dir(curr_dir, next_dir):
    dirs = {'n': 0, 'e': 1, 's': 2, 'w': 3}
    motion = [None, 'left', 'about-face', 'right']
    diff = (dirs[curr_dir] - dirs[next_dir]) % 4
    return motion[diff]

def shootable(dir, wombat, tile, arena_size, shot_range):
    x_wom, y_wom = wombat['x'], wombat['y']
    x_tar, y_tar = tile['x'], tile['y']
    if x_wom == x_tar and y_wom == y_tar:
        return False
    if dir == 'n':
        return x_wom == x_tar and (shot_range >= (y_wom - y_tar) % arena_size)
    elif dir == 'e':
        return y_wom == y_tar and (shot_range >= (x_tar - x_wom) % arena_size)
    elif dir == 's':
        return x_wom == x_tar and (shot_range >= (y_tar - y_wom) % arena_size)
    elif dir == 'w':
        return y_wom == y_tar and (shot_range >= (x_wom - x_tar) % arena_size)
    else:
        return False

def can_shoot(dir, arena, arena_size, shot_range, wombat={'x': 3, 'y': 3}, wall=True):
    filters = ['zakano', 'wombat']
    filters += ['wood-barrier', 'steel-barrier'] if wall else []
    targets = filter_arena(arena, filters)
    return [tile for tile in targets
            if shootable(dir, wombat, tile, arena_size, shot_range)] != []

def possible_points(arena, wombat={'x': 3, 'y': 3}, wall=True):
    arena = add_locs(arena)
    filters = ['zakano', 'wombat', 'food']
    filters += ['wood-barrier', 'steel-barrier'] if wall else []
    possible = filter_arena(arena, filters)
    x, y = wombat['x'], wombat['y']
    return [tile for tile in possible if (tile['x'] != x or tile['y'] != y)]

def build_resp(action, direction=None):
    if direction is None:
        return {'action': action, 'metadata': {}}
    else:
        return {'action': action, 'metadata': {'direction': direction}}

def new_direction(direction, loc, wombat, arena_half):
    dirs = ['n', 'e', 's', 'w']
    dirs.remove(direction)
    positions = [dir for dir in dirs if is_facing(dir, loc, arena_half, wombat)]
    if positions != []:
        return turn_to_dir(direction, positions[0])
    else:
        # TODO: implement logic here
        return 'left'

def front_tile(dir, arena_size, wombat={'x': 3, 'y': 3}):
    x = wombat['x']
    y = wombat['y']
    if dir == 'n':
        return {'x': x, 'y': (y - 1) % arena_size}
    elif dir == 'e':
        return {'x': (x + 1) % arena_size, 'y': y}
    elif dir == 's':
        return {'x': x, 'y': (y + 1) % arena_size}
    elif dir == 'w':
        return {'x': (x - 1) % arena_size, 'y': y}
    else:
        # This should never happen
        return {'x': x, 'y': y}

def is_clear(arena, wombat):
    blockers = ['zakano', 'wombat', 'wood-barrier', 'steel-barrier', 'poison']
    x, y = wombat['x'], wombat['y']
    return arena[y][x]['contents']['type'] not in blockers

def move_to(arena, arena_half, dir, loc, wombat={'x': 3, 'y': 3}):
    if is_facing(dir, loc, arena_half, wombat) and is_clear(
      arena, front_tile(dir, arena_half * 2, wombat)):
        return build_resp('move')
    else:
        return build_resp('turn', new_direction(dir, loc, wombat, arena_half))

def focus_sight(arena):
    return [row[1:7] for row in arena[1:7]]

def select_target(arena, arena_size, wombat={'x': 3, 'y': 3}, wall=True):
    possible = possible_points(arena, wombat, wall=wall)
    if possible == []:
        return None
    direction = get_direction(arena)
    for tile in possible:
        tile['dist'] = distance_to_tile(direction, tile, arena_size, wombat)
    return sorted(possible, key=lambda x: x['dist'])[0]
