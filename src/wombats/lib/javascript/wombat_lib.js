// Have module.exports be a function so that user won't have to worry about namespaces
const turn_directions = ['right', 'left', 'about-face'];
const smoke_directions = ['forward', 'backward', 'left', 'right', 'drop'];
const game_parameters = {
  // HP Modifiers
  'collision-hp-damage': 10,
  'food-hp-bonus': 5,
  'poison-hp-damage': 10,
  // Score Modifiers
  'food-score-bonus': 10,
  'wombat-hit-bonus': 10,
  'zakano-hit-bonus': 8,
  'steel-barrier-hit-bonus': 2,
  'wood-barrier-hit-bonus': 2,
  'wombat-destroyed-bonus': 25,
  'zakano-destroyed-bonus': 15,
  'wood-barrier-destroyed-bonus': 3,
  'steel-barrier-destroyed-bonus': 25,
  // In game parameters
  'shot-distance': 5
};
const shot_range = game_parameters['shot-distance'];
const zakano_key = 'zakano';
const wombat_key = 'wombat';
const steel_key = 'steel-barrier';
const wood_key = 'wood-barrier';
const food_key = 'food';
const poison_key = 'poison';
const fog_key = 'fog';
const open_key = 'open';

const point_sources = [zakano_key, wombat_key, steel_key, wood_key, food_key];
const points_no_wall = [zakano_key, wombat_key, food_key];
const enemies = [wombat_key, zakano_key];
const blockers = [zakano_key, wombat_key, wood_key, steel_key, poison_key];
const targets = [zakano_key, wombat_key, steel_key, wood_key];
const persistent = [food_key, poison_key, open_key, wood_key, steel_key];

const default_tile = {contents: {type: fog_key}};

const mod = function(n, m) {
  // JS doesn't handle taking the modulus of negative numbers the way python and clojure do
  // Technically `%` is remainder operator, not modulus operator
  return ((n % m) + m) % m;
};

const copy_arena = function(arena) {
  // Returns a shallow copy of the arena
  return arena.map(row => {
    return row.map(tile => {
      return Object.assign({}, tile);
    });
  });
}

const get_arena_size = function(state) {
  // Fetches the size of the arena and returns it as an object with the keys:
  //    width
  //    height
  return {
    width: state['global-dimensions'][0],
    height: state['global-dimensions'][1]
  };
};

const add_locs = function(arena) {
  // Add local x and y coordinates to the arena matrix
  arena = copy_arena(arena)
  for(let y = 0; y < arena.length; y++) {
    const row = arena[y];
    for(let x = 0; x < row.length; x++) {
      arena[y][x].x = x;
      arena[y][x].y = y;
    };
  };
  return arena;
};

const filter_arena = function(arena, filters = null) {
  // Filter the arena to return only nodes that contain one of the supplied types
  if (filters == null) {
    // With no filters returned flattened arena
    return [].concat.apply([], arena);
  } else {
    let flattened = [].concat.apply([], arena);
    return flattened.filter(function(tile) {
      return filters.includes(tile.contents.type);
    });
  }
};

const build_initial_global_state = function(global_size) {
  // Construct an initial global state populated by fog
  const matrix = new Array(global_size.height);
  for(let i = 0; i < global_size; i++) {
    matrix[i] = new Array(global_size.width);
    for(let j = 0; j < global_size; j++) {
      // Need to get a shallow copy of the default tile so that they can be altered independently
      matrix[i][j] = Object.assign({}, default_tile);
    }
  }
  return matrix;
};

const add_to_state = function(arena, elem) {
  // Update the global state with the given element
  arena[elem.y][elem.x] = elem;
  return arena;
};

const merge_global_state = function(global_arena, local_state, global_size) {
  // Add local state vision to global saved state
  let x_offset = (local_state['global-coords'][0] - 3) % global_size.height;
  let y_offset = (local_state['global-coords'][1] - 3) % global_size.width;
  let local_tiles = filter_arena(add_locs(local_state.arena), persistent);

  let wombat = {contents: {type: open_key},
                x: local_state['global-coords'][0],
                y: local_state['global-coords'][1]
  };

  for (let i = 0; i < local_tiles.length; i++) {
    let tile = local_tiles[i];
    tile.x = (tile.x + x_offset) % global_size.width;
    tile.y = (tile.y + y_offset) % global_size.height;
    global_arena = add_to_state(global_arena, tile);
  }

  return add_to_state(global_arena, wombat);
};

const get_global_state = function(state, path) {
  // Tries to fetch the global arena from the saved state or constructs a new one
  // Path is the location of the saved arena from the state object
  for (level in path) {
    state = state[path[level]];
    if (null === state) {
      return build_initial_global_state(get_arena_size(state));
    }
  }
  return temp;
};

const get_direction = function(arena) {
  // Gets the current direction of your wombat from the 2d arena array
  // Assumes local arena vision will always be 7x7
  return arena[3][3].contents.orientation;
};

const is_facing = function(dir, target, arena_size, wombat={x: 3, y:3}) {
  // Returns true if a move forward will bring you closer to the target location
  // If no wombat coordinates are provided, use distance from (3, 3)
  const x_half = arena_size.width / 2;
  const y_half = arena_size.height / 2;
  switch(dir) {
    case 'n':
      return (target.y != wombat.y) && (y_half >= mod(wombat.y - target.y, (y_half * 2)));
    case 'e':
      return (target.x != wombat.x) && (x_half >= mod(target.x - wombat.x, (x_half * 2)));
    case 's':
      return (target.y != wombat.y) && (y_half >= mod(target.y - wombat.y, (y_half * 2)));
    case 'w':
      return (target.x != wombat.x) && (x_half >= mod(wombat.x - target.x, (x_half * 2)));
    default:
      return false;
  }
};

const distance_to_tile = function(dir, node, arena_size, wombat={x: 3, y:3 }) {
  // Gets the minimum number of moves it would take to travel to a given tile
  // Does not take into account any objects that might be in the way
  // Will you need to turn to face location?
  const facing = is_facing(dir, node, arena_size, wombat) ? 0 : 1;
  // Minimum of actual distance or distance with wraparound
  const x_dist = Math.min(Math.abs(node.x - wombat.x),
                        arena_size.width + Math.min(node.x, wombat.x) - Math.max(node.x, wombat.x));
  const y_dist = Math.min(Math.abs(node.y - wombat.y),
                        arena_size.height + Math.min(node.y, wombat.y) - Math.max(node.y, wombat.y));
  // Will you need to turn again?
  const turn = 0 == Math.min(x_dist, y_dist) ? 0 : 1;
  return x_dist + y_dist + facing + turn;
};

const turn_to_dir = function(curr_dir, next_dir) {
  // Given the current cardinal direction and the desired cardinal direction
  // Returns the turn motion needed
  const dirs = {n: 0, e: 1, s: 2, w: 3};
  const motion = [null, 'left', 'about-face', 'right'];
  const diff = mod(dirs[curr_dir] - dirs[next_dir], 4);
  return motion[diff];
}

const shootable = function(dir, wom, tile, arena_size) {
  // Helper function to determine if wombat can shoot given tile
  if (wom.x === tile.x && wom.y === tile.y) {
    // You can't shoot yourself
    return false;
  }
  switch(dir) {
    // For each direction, are you directly facing given tile and is it within shot_range?
    case 'n':
      return wom.x === tile.x && (shot_range >= mod(wom.y - tile.y, arena_size.height));
    case 'e':
      return wom.y === tile.y && (shot_range >= mod(tile.x - wom.x, arena_size.width));
    case 's':
      return wom.x === tile.x && (shot_range >= mod(tile.y - wom.y, arena_size.height));
    case 'w':
      return wom.x === tile.x && (shot_range >= mod(wom.y - tile.y, arena_size.width));
    default:
      return false;
  }
};

const can_shoot = function(dir, arena, arena_size, wombat={x: 3, y: 3}, wall=true) {
  // Searches arena to determine if any tile is shootable
  // If wall is false, doesn't include steel-barrier or wood-barrier
  arena = add_locs(arena)
  const filters = wall ? targets : enemies;
  const possible_targets = filter_arena(arena, filters);
  return possible_targets.filter(function(tile) {
    return shootable(dir, wombat, tile, arena_size);
  }).length != 0;
};

const possible_points = function(arena, wombat={x: 3, y: 3}, wall=true) {
  // Returns a list of all possible point sources within field of vision
  // If wall is false, doesn't include steel-barrier or wood-barrier
  arena = add_locs(arena);
  const filters = wall ? point_sources : points_no_wall;
  const possible = filter_arena(arena, filters);
  
  return possible.filter(function(tile) {
    return tile.x != wombat.x || tile.y != wombat.y; 
  });
};

const build_command = function(action, direction=null) {
  // Helper function to create a well formed command
  if (direction == null) {
    return {action: action, metadata: {}};
  }
  else {
    return {action: action, metadata: {direction: direction}};
  }
};

const build_resp = function(command, state=null) {
  // Helper function to create a well formed response
  if (state == null) {
    return {command: command, state: {}};
  }
  else {
    return {command: command, state: state};
  }
};

const new_direction = function(dir, loc, wombat, arena_size) {
  // Returns which way to turn to face target location
  // Currently returns null if no direction is facing target
  const dirs = ['n', 'e', 's', 'w'];
  dirs.splice(dirs.indexOf(dir), 1);
  const positions = dirs.filter(function(dir) {
    return is_facing(dir, loc, arena_size, wombat);
  });
  if (positions.length == 0) {
    return null;
  } else {
    return turn_to_dir(dir, positions[0]);
  }
};

const front_tile = function(dir, arena_size, wombat={x: 3, y: 3}) {
  // Returns an object with x and y properties of the tile directly in front of the wombat
  const x = wombat.x, y = wombat.y;
  switch (dir) {
    case 'n':
      return {x: x, y: mod(y - 1, arena_size.height)};
    case 'e':
      return {x: mod(x + 1, arena_size.width), y: y};
    case 's':
      return {x: x, y: mod(y + 1, arena_size.height)};
    case 'w':
      return {x: mod(x - 1, arena_size.width), y: y};
    default:
      return {x: x, y: y};
  }
};

const is_clear = function(arena, tile) {
  // Returns true if wombat can safely occupy given tile, false otherwise
  arena = add_locs(arena)
  const x = tile.x, y = tile.y;
  return !blockers.includes(arena[y][x].contents.type);
};

const move_to = function(arena, arena_size, dir, loc, wombat={x: 3, y: 3}) {
  // Returns the command to move wombat closer to target location
  // If cannot move forward and directly facing location returns null
  if (is_facing(dir, loc, arena_size, wombat) && is_clear(arena, front_tile(dir, arena_size, wombat))) {
    return build_command('move');
  }  
  else {
    const new_dir = new_direction(dir, loc, wombat, arena_size);
    return (new_dir === null) ? null :  build_command('turn', new_dir);
  }
};

const focus_sight = function(arena) {
  arena = add_locs(arena);
  // Cut the 7x7 arena down to 5x5
  let temp = arena.map(function(row) {
    return row.splice(1, 5);
  });
  return temp.splice(1, 5);
};

const select_target = function(arena, arena_size, wombat={x: 3, y: 3}, wall=true) {
  // Returns the closest point source to the wombat by number of moves away
  // Does not take into account anything blocking the path or total possible value
  const possible = possible_points(arena, wombat, wall);
  if (possible.length === 0) {
    return null;
  }
  const direction = get_direction(arena);
  for (tile in possible) {
    possible[tile].dist = distance_to_tile(direction, possible[tile], arena_size, wombat);
  }
  // Sort by distance
  return possible.sort((a, b) => {
    return a.dist - b.dist;
  })[0];
};

module.exports = function() {
  // Useful constants
  this.turn_directions = turn_directions;
  this.smoke_directions = smoke_directions;
  this.game_parameters = game_parameters;
  this.shot_range = shot_range;
  this.zakano_key = zakano_key;
  this.wombat_key = wombat_key;
  this.steel_key = steel_key;
  this.wood_key = wood_key;
  this.food_key = food_key;
  this.poison_key = poison_key;
  this.fog_key = fog_key
  this.open_key = open_key

  // Lists of objects to use as filters
  this.point_sources = point_sources;
  this.points_no_wall = points_no_wall;
  this.enemies = enemies;
  this.blockers = blockers;
  this.targets = targets;
  this.persistent = persistent;

  this.default_tile = default_tile

  // Helper functions
  this.mod = mod;
  this.copy_arena = copy_arena;
  this.get_arena_size = get_arena_size;
  this.build_command = build_command;
  this.build_resp = build_resp;

  // Global State Manipulations
  this.build_initial_global_state = build_initial_global_state;
  this.add_to_state = add_to_state;
  this.merge_global_state = merge_global_state;
  this.get_global_state = get_global_state;
  
  // State manipulations
  this.filter_arena = filter_arena;
  this.add_locs = add_locs;
  this.focus_sight = focus_sight;
  this.get_direction = get_direction;  

  // Handle management of directions
  this.is_facing = is_facing;
  this.new_direction = new_direction;
  this.front_tile = front_tile;
  this.turn_to_dir = turn_to_dir;

  // Specific tile fitness functions
  this.distance_to_tile = distance_to_tile;
  this.is_clear = is_clear;
  this.move_to = move_to;
  this.shootable = shootable;

  // Helper functions to evaluate moves
  this.can_shoot = can_shoot;
  this.possible_points = possible_points;
  this.select_target = select_target;
};
