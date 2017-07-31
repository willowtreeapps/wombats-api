// Have module.exports be a function so that user won't have to worry about namespaces
module.exports = function() {
  this.turn_directions = ['right', 'left', 'about-face'];
  this.smoke_directions = ['forward', 'backward', 'left', 'right', 'drop'];
  this.game_parameters = {
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
  this.shot_range = game_parameters['shot-distance'];
  this.zakano_key = 'zakano';
  this.wombat_key = 'wombat';
  this.steel_key = 'steel-barrier';
  this.wood_key = 'wood-barrier';
  this.food_key = 'food';
  this.poison_key = 'poison';
  this.fog_key = 'fog';
  this.open_key = 'open';

  this.point_sources = [zakano_key, wombat_key, steel_key, wood_key, food_key];
  this.points_no_wall = [zakano_key, wombat_key, food_key];
  this.enemies = [wombat_key, zakano_key];
  this.blockers = [zakano_key, wombat_key, wood_key, steel_key, poison_key];
  this.targets = [zakano_key, wombat_key, steel_key, wood_key];
  this.persistent = [food_key, poison_key, open_key, wood_key, steel_key];

  this.default_tile = {contents: {type: fog_key}};

  this.mod = function(n, m) {
    // JS doesn't handle taking the modulus of negative numbers the way python and clojure do
    return ((n % m) + m) % m;
  };

  this.get_arena_size = function(state) {
    return {
      width: state['global-dimensions'][0],
      height: state['global-dimensions'][1]
    };
  };

  this.add_locs = function(arena) {
    var arena = JSON.parse(JSON.stringify(arena))
    for(y = 0; y < arena.length; y++) {
      var row = arena[y];
      for(x = 0; x < row.length; x++) {
        arena[y][x].x = x;
        arena[y][x].y = y;
      };
    };
    return arena;
  };

  this.filter_arena = function(arena, filters = null) {
    if (filters == null) {
      return [].concat.apply([], arena);
    } else {
      var flattened = [].concat.apply([], arena);
      return flattened.filter(function(tile) {
        return filters.includes(tile.contents.type);
      });
    }
  };

  this.build_initial_global_state = function(global_size) {
    var matrix = new Array(global_size.height);
    for(i = 0; i < global_size; i++) {
      matrix[i] = new Array(global_size.width);
      for(j = 0; j < global_size; j++) {
        // Need to get a deep copy of the default tile so that they can be altered independently
        matrix[i][j] = JSON.parse(JSON.stringify(default_tile));
      }
    }
    return matrix;
  };

  this.add_to_state = function(arena, elem) {
    arena[elem.y][elem.x] = elem;
    return arena;
  };

  this.merge_global_state = function(global_arena, local_state, global_size) {
    var x_offset = (local_state['global-coords'][0] - 3) % global_size.height;
    var y_offset = (local_state['global-coords'][1] - 3) % global_size.width;
    var local_tiles = filter_arena(add_locs(local_state.arena), persistent);

    var wombat = {contents: {type: open_key},
                  x: local_state['global-coords'][0],
                  y: local_state['global-coords'][1]
    };

    for (i = 0; i < local_tiles.length; i++) {
      var tile = local_tiles[i];
      tile.x = (tile.x + x_offset) % global_size.width;
      tile.y = (tile.y + y_offset) % global_size.height;
      global_arena = add_to_state(global_arena, tile);
    }

    return add_to_state(global_arena, wombat);
  };

  this.get_global_state = function(state, path) {
    var temp = state;
    for (level in path) {
      temp = temp[path[level]];
      if (null == temp) {
        return build_initial_global_state(get_arena_size(state));
      }
    }
    return temp;
  };

  this.get_direction = function(arena) {
    return arena[3][3].contents.orientation;
  };
  
  this.is_facing = function(dir, target, arena_size, wombat={x: 3, y:3}) {
    var x_half = arena_size.width / 2;
    var y_half = arena_size.height / 2;
    switch(dir) {
      case 'n':
        return (target.y != wombat.y) && (y_half >= mod(wombat.y - target.y, (y_half * 2)));
        break;
      case 'e':
        return (target.x != wombat.x) && (x_half >= mod(target.x - wombat.x, (x_half * 2)));
        break;
      case 's':
        return (target.y != wombat.y) && (y_half >= mod(target.y - wombat.y, (y_half * 2)));
        break;
      case 'w':
        return (target.x != wombat.x) && (x_half >= mod(wombat.x - target.x, (x_half * 2)));
        break;
      default:
        return false;
    }
  };

  this.distance_to_tile = function(dir, node, arena_size, wombat={x: 3, y:3 }) {
    var facing = is_facing(dir, node, arena_size, wombat) ? 0 : 1;
    var x_dist = Math.min(Math.abs(node.x - wombat.x),
                          arena_size.width + Math.min(node.x, wombat.x) - Math.max(node.x, wombat.x));
    var y_dist = Math.min(Math.abs(node.y - wombat.y),
                          arena_size.height + Math.min(node.y, wombat.y) - Math.max(node.y, wombat.y));
    var turn = 0 == Math.min(x_dist, y_dist) ? 0 : 1;
    return x_dist + y_dist + facing + turn;
  };

  this.turn_to_dir = function(curr_dir, next_dir) {
    var dirs = {n: 0, e: 1, s: 2, w: 3};
    var motion = [null, 'left', 'about-face', 'right'];
    var diff = mod(dirs[curr_dir] - dirs[next_dir], 4);
    return motion[diff];
  }

  this.shootable = function(dir, wom, tile, arena_size) {
    if (wom.x == tile.x && wom.y == tile.y) {
      return false;
    }
    switch(dir) {
      case 'n':
        return wom.x == tile.x && (shot_range >= mod(wom.y - tile.y, arena_size.height));
        break;
      case 'e':
        return wom.y == tile.y && (shot_range >= mod(tile.x - wom.x, arena_size.width));
        break;
      case 's':
        return wom.x == tile.x && (shot_range >= mod(tile.y - wom.y, arena_size.height));
        break;
      case 'w':
        return wom.x == tile.x && (shot_range >= mod(wom.y - tile.y, arena_size.width));
        break;
      default:
        return false;
    }
  };

  this.can_shoot = function(dir, arena, arena_size, wombat={x: 3, y: 3}, wall=true) {
    arena = add_locs(arena)
    var filters = wall ? targets : enemies;
    var possible_targets = filter_arena(arena, filters);
    return possible_targets.filter(function(tile) {
      return shootable(dir, wombat, tile, arena_size);
    }).length != 0;
  };

  this.possible_points = function(arena, wombat={x: 3, y: 3}, wall=true) {
    arena = add_locs(arena);
    var filters = wall ? point_sources : points_no_wall;
    var possible = filter_arena(arena, filters);
    
    return possible.filter(function(tile) {
      return tile.x != wombat.x || tile.y != wombat.y; 
    });
  };

  this.build_resp = function(action, direction=null) {
    if (direction == null) {
      return {action: action, metadata: {}};
    }
    else {
      return {action: action, metadata: {direction: direction}};
    }
  };

  this.new_direction = function(dir, loc, wombat, arena_size) {
    var dirs = ['n', 'e', 's', 'w'];
    dirs.splice(dirs.indexOf(dir), 1);
    var positions = dirs.filter(function(dir) {
      return is_facing(dir, loc, arena_size, wombat);
    });
    if (positions.length == 0) {
      return 'left';
    } else {
      return turn_to_dir(dir, positions[0]);
    }
  };

  this.front_tile = function(dir, arena_size, wombat={x: 3, y: 3}) {
    var x = wombat.x, y = wombat.y;
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

  this.is_clear = function(arena, tile) {
    arena = add_locs(arena)
    var x = tile.x, y = tile.y;
    return !blockers.includes(arena[y][x].contents.type);
  };

  this.move_to = function(arena, arena_size, dir, loc, wombat={x: 3, y: 3}) {
    if (is_facing(dir, loc, arena_size, wombat) && is_clear(arena, front_tile(dir, arena_size, wombat))) {
      return build_resp('move');
    }  
    else {
      return build_resp('turn', new_direction(dir, loc, wombat, arena_size));
    }
  };

  this.focus_sight = function(arena) {
    arena = add_locs(arena);
    var temp = arena.map(function(row) {
      return row.splice(1, 5);
    });
    return temp.splice(1, 5);
  };

  this.select_target = function(arena, arena_size, wombat={x: 3, y: 3}, wall=true) {
    var possible = possible_points(arena, wombat, wall);
    if (possible.length == 0) {
      return null;
    }
    var direction = get_direction(arena);
    for (tile in possible) {
      possible[tile].dist = distance_to_tile(direction, possible[tile], arena_size, wombat);
    }
    return possible.sort(function(a, b) {
      return a.dist - b.dist;
    })[0];
  };
};
