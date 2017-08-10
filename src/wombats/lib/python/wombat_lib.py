from copy import deepcopy, copy
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

shot_range = game_parameters['shot-distance']

zakano_key = 'zakano'
wombat_key = 'wombat'
steel_key = 'steel-barrier'
wood_key = 'wood-barrier'
food_key = 'food'
poison_key = 'poison'
fog_key = 'fog'
open_key = 'open'
smoke_key = 'smoke'

point_sources = [zakano_key, wombat_key, steel_key, wood_key, food_key]
points_no_wall = [zakano_key, wombat_key, food_key]
enemies = [wombat_key, zakano_key]
blockers = [zakano_key, wombat_key, wood_key, steel_key, poison_key]
targets = [zakano_key, wombat_key, steel_key, wood_key]
persistent = [food_key, poison_key, open_key, wood_key, steel_key]

default_tile = {'contents': {'type': fog_key}}

def get_arena_size(state):
    '''
    Fetches the size of the arena and returns it as a dictionary with keys:
        width
        height
    '''
    return {
        'width': state['global-dimensions'][0],
        'height': state['global-dimensions'][1]
    }

def add_locs(arena):
    '''Add local x and y coordinates to the arena matrix'''
    arena = deepcopy(arena)
    for x in range(len(arena)):
        for y in range(len(arena[x])):
            arena[y][x]['x'] = x
            arena[y][x]['y'] = y
    return arena

def filter_arena(arena, filters=None):
    '''
    Filter the arena to return only nodes that contain one of the supplied types
    '''
    if filters is None:
        # With no filters returns the flattened arena
        return [tile for row in arena for tile in row]
    else:
        return [tile for row in arena for tile in row if tile['contents']['type'] in filters]

def build_initial_global_state(global_size):
    '''Constructs an intitial global state populated by fog'''
    return [[copy(default_tile) for _ in range(global_size['width'])]
            for _ in range(global_size['height'])]

def add_to_state(arena, elem):
    '''Update the global state with the given element'''
    # Modifies the arena object passed in and returns it
    arena[elem['y']][elem['x']] = elem
    return arena

def merge_global_state(global_arena, local_state, global_size):
    '''Add local state vision to global saved state.'''
    x_offset = (local_state['global-coords'][0] - 3) % global_size['width']
    y_offset = (local_state['global-coords'][1] - 3) % global_size['height']
    local_tiles = filter_arena(add_locs(local_state['arena']), persistent)

    # We want to add that the tile the player currently occupies is open
    wombat = {'contents': {'type': open_key},
              'x': local_state['global-coords'][0],
              'y': local_state['global-coords'][1]
              }
    for tile in local_tiles:
        tile['x'] = (tile['x'] + x_offset) % global_size['width']
        tile['y'] = (tile['y'] + y_offset) % global_size['height']
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
        return build_initial_global_state(get_arena_size(state))
    else:
        return temp

def get_direction(arena):
    '''
    Gets the current direction of your wombat from the 2d arena array
    Assumes local arena vision will always be 7x7
    '''
    return arena[3][3]['contents']['orientation']

def is_facing(dir, target, arena_size, wombat={'x': 3, 'y': 3}):
    '''
    Returns true if a move forward will bring you closer to the target location
    If no wombat coordinates are provided, use distance from (3, 3)
    '''
    x_half = arena_size['width'] / 2
    y_half = arena_size['height'] / 2
    if dir == 'n':
        return ((target['y'] != wombat['y']) and
                (y_half >= ((wombat['y'] - target['y']) % (y_half * 2))))
    elif dir == 'e':
        return ((target['x'] != wombat['x']) and
                (x_half >= ((target['x'] - wombat['x']) % (x_half * 2))))
    elif dir == 's':
        return ((target['y'] != wombat['y']) and
                (y_half >= ((target['y'] - wombat['y']) % (y_half * 2))))
    elif dir == 'w':
        return ((target['x'] != wombat['x']) and
                (x_half >= ((wombat['x'] - target['x']) % (x_half * 2))))
    else:
        # This shouldn't happen
        return False

def distance_to_tile(dir, node, arena_size, wombat={'x': 3, 'y': 3}):
    '''
    Gets the minimum number of moves it would take to travel to a given tile
    Does not take into account anything that might be in the way
    '''
    # Will you need to turn to face location?
    facing = 0 if is_facing(dir, node, arena_size, wombat) else 1
    # Minimum of actual distance or distance with wraparound
    x_dist = min(abs(node['x'] - wombat['x']), arena_size['width'] +
                 min(node['x'], wombat['x']) - max(node['x'], wombat['x']))
    y_dist = min(abs(node['y'] - wombat['y']), arena_size['height'] +
                 min(node['y'], wombat['y']) - max(node['y'], wombat['y']))
    # Will you need to turn again
    turn = 0 if min(x_dist, y_dist) == 0 else 1
    return x_dist + y_dist + facing + turn

def turn_to_dir(curr_dir, next_dir):
    '''
    Given the current cardinal direction and the desired cardinal direction
    Returns the turn motion needed
    '''
    dirs = {'n': 0, 'e': 1, 's': 2, 'w': 3}
    motion = [None, 'left', 'about-face', 'right']
    diff = (dirs[curr_dir] - dirs[next_dir]) % 4
    return motion[diff]

def shootable(dir, wombat, tile, arena_size):
    '''
    Helper function to determine if wombat can shoot given tile
    '''
    x_wom, y_wom = wombat['x'], wombat['y']
    x_tar, y_tar = tile['x'], tile['y']
    x_size = arena_size['width']
    y_size = arena_size['height']
    if x_wom == x_tar and y_wom == y_tar:
        # You can't shoot yourself
        return False
    elif dir == 'n':
        return x_wom == x_tar and (shot_range >= (y_wom - y_tar) % y_size)
    elif dir == 'e':
        return y_wom == y_tar and (shot_range >= (x_tar - x_wom) % x_size)
    elif dir == 's':
        return x_wom == x_tar and (shot_range >= (y_tar - y_wom) % y_size)
    elif dir == 'w':
        return y_wom == y_tar and (shot_range >= (x_wom - x_tar) % x_size)
    else:
        return False

def can_shoot(dir, arena, arena_size, wombat={'x': 3, 'y': 3}, wall=True):
    '''
    Searches the list of all tiles to determine if any tile is shootable
    If wall is False, doesn't include steel-barrier or wood-barrier
    '''
    arena = add_locs(arena)
    filters = targets if wall else enemies
    possible_targets = filter_arena(arena, filters)
    return [tile for tile in possible_targets
            if shootable(dir, wombat, tile, arena_size)] != []

def possible_points(arena, wombat={'x': 3, 'y': 3}, wall=True):
    '''
    Returns a list of all possible point sources within field of vision
    If wall is False, doesn't include steel-barrier or wood-barrier
    '''
    arena = add_locs(arena)
    filters = point_sources if wall else points_no_wall
    possible = filter_arena(arena, filters)
    # Return list of tiles minus the wombat itself
    x, y = wombat['x'], wombat['y']
    return [tile for tile in possible if (tile['x'] != x or tile['y'] != y)]

def build_resp(action, direction=None):
    '''
    Helper function to create a well formed response object
    '''
    if direction is None:
        return {'action': action, 'metadata': {}}
    else:
        return {'action': action, 'metadata': {'direction': direction}}

def new_direction(direction, loc, wombat, arena_size):
    '''
    Returns which direction to turn to face target location
    Currently returns None if no direction is facing target
    '''
    dirs = ['n', 'e', 's', 'w']
    dirs.remove(direction)
    positions = [dir for dir in dirs if is_facing(dir, loc, arena_size, wombat)]
    if positions != []:
        return turn_to_dir(direction, positions[0])
    else:
        return None

def front_tile(dir, arena_size, wombat={'x': 3, 'y': 3}):
    '''
    Returns the a dictionary containing keys 'x' and 'y' representing the tile
    directly in front of the wombat
    '''
    x = wombat['x']
    y = wombat['y']
    if dir == 'n':
        return {'x': x, 'y': (y - 1) % arena_size['height']}
    elif dir == 'e':
        return {'x': (x + 1) % arena_size['width'], 'y': y}
    elif dir == 's':
        return {'x': x, 'y': (y + 1) % arena_size['height']}
    elif dir == 'w':
        return {'x': (x - 1) % arena_size['width'], 'y': y}
    else:
        # This should never happen
        return {'x': x, 'y': y}

def is_clear(arena, tile):
    '''
    Returns true if wombat can safely occupy given tile, returns false otherwise
    '''
    x, y = tile['x'], tile['y']
    return arena[y][x]['contents']['type'] not in blockers

def move_to(arena, arena_size, dir, loc, wombat={'x': 3, 'y': 3}):
    '''
    Returns the command to move closer to target location
    If cannot move forward and directly facing location returns None
    '''
    if is_facing(dir, loc, arena_size, wombat) and is_clear(
      arena, front_tile(dir, arena_size, wombat)):
        return build_resp('move')
    else:
        new_dir = new_direction(dir, loc, wombat, arena_size)
        return build_resp('turn', new_dir) if new_dir is not None else None

def focus_sight(arena):
    '''
    Changes the 7x7 local vision to 5x5 to focus on closer objects
    '''
    return [row[1:5] for row in arena[1:5]]

def select_target(arena, arena_size, wombat={'x': 3, 'y': 3}, wall=True):
    '''
    Returns the closest point source to the wombat, by number of moves it
        would take to reach location
    Does not take into account anything blocking the path or total possible value
    '''
    possible = possible_points(arena, wombat, wall=wall)
    if possible == []:
        return None
    direction = get_direction(arena)
    for tile in possible:
        tile['dist'] = distance_to_tile(direction, tile, arena_size, wombat)
    return sorted(possible, key=lambda x: x['dist'])[0]
