# Wombats Library for Javascript

## Overview
This is standard library for wombats, including functions that you may find helpful as a starting point or base for your own wombat. This library is not intended to include any complex logic or decision making -- it's just to help you get started with your wombat and remove the burden of common state manipulations you might you need.

#### All Functions

- [add\_locs()](#add_locsarena)
- [add\_to\_state()](#add_to_statearena-elem)
- [build\_command()](#build_commandaction-directionnull)
- [build\_initial\_global\_state()](#build_initial_global_stateglobal_size)
- [build\_resp()](#build_respcommand-statenull)
- [can\_shoot()](#can_shootdir-arena-arena_size-wombatx-3-y-3-walltrue)
- [copy\_arena()](#copy_arenaarena)
- [distance\_to\_tile()](#distance_to_tiledir-node-arena_size-wombatx-3-y-3)
- [filter\_arena()](#filter_arenaarena-filtersnull)
- [focus\_sight()](#focus_sightarena)
- [front\_tile()](#front_tiledir-arena_size-wombatx-3-y-3)
- [get\_arena\_size()](#get_arena_sizestate)
- [get\_direction()](#get_directionarena)
- [get\_global\_state()](#get_global_statestate-path)
- [is\_clear()](#is_cleararena-tile)
- [is\_facing()](#is_facingdir-target-arena_size-wombatx-3-y-3)
- [merge\_global\_state()](#merge_global_stateglobal_arena-local_state-global_size)
- [mod()](#modn-m)
- [move\_to()](#move_toarena-arena_size-dir-loc-wombatx-3-y-3)
- [new\_direction()](#new_directiondirection-loc-wombat-arena_size)
- [possible\_points()](#possible_pointsarena-wombatx-3-y-3-walltrue)
- [select\_target()](#select_targetarena-arena_size-wombatx-3-y-3-walltrue)
- [shootable()](#shootabledir-wombat-tile-arena_size)
- [turn\_to\_dir()](#turn_to_dircurr_dir-next_dir)

### Constants

```javascript
// There are 3 ways to turn
const turn_directions = ['right', 'left', 'about-face']
// There are 5 ways to throw a smoke bomb
const smoke_directions = ['forward', 'backward', 'left', 'right', 'drop']
// Game parameters taken from API itself
const game_parameters = {
    //HP Modifiers
    'collision-hp-damage': 10,
    'food-hp-bonus': 5,
    'poison-hp-damage': 10,
    //Score Modifiers
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
}

// Pull out the number of tiles gunfire will travel
const shot_range = game_parameters['shot-distance']

// The possible properties representing the 'type' of the 'contents' object within each tile
const zakano_key = 'zakano'
const wombat_key = 'wombat'
const steel_key = 'steel-barrier'
const wood_key = 'wood-barrier'
const food_key = 'food'
const poison_key = 'poison'
const fog_key = 'fog'
const open_key = 'open'
const smoke_key = 'smoke'

// Different lists of tile contents to be used as filters
const point_sources = [zakano_key, wombat_key, steel_key, wood_key, food_key]
const points_no_wall = [zakano_key, wombat_key, food_key]
const enemies = [wombat_key, zakano_key]
const blockers = [zakano_key, wombat_key, wood_key, steel_key, poison_key]
const targets = [zakano_key, wombat_key, steel_key, wood_key]
const persistent = [food_key, poison_key, open_key, wood_key, steel_key]

// The default tile used when creating the initial global state
const default_tile = {'contents': {'type': fog_key}}
```

### Functions

### `add_locs(arena)`
**This function takes in the matrix of objects representing the arena and annotates each tile with `x`, and `y` properties whose value correspond to the x and y coordinates of the tile within the matrix**
##### Params
- `arena`

  A 2 dimensional array of objects representing the game arena
##### Usage
```javascript
// state is the top level parameter in wombat function
const annotated_arena = add_locs(state.arena)
```
##### Returns
Returns a deep copy of the arena matrix with each tile containing `x` and `y` properties

### `add_to_state(arena, elem)`
**Takes in an arena and a single tile as an object and returns the arena with element at (x, y) replaced by the tile**
##### Params
- `arena`

  The two dimensional array of objects representing the game arena

- `elem`

  An object representing a single tile of the arena; _MUST HAVE PROPERTIES `x` and `y'_

##### Usage
```javascript
// state is the top level parameter in the wombat function
const tile = {contents: {type: 'open'},
        x: 2, y: 4
       }
const arena = add_to_state(state.arena, tile) 
// Returns the arena with the dict at state.arena[4][2] replaced by tile
```
##### Returns
Modifies the arena in place as well as returning the new 2d array representing the arena


### `build_command(action, direction=null)`
**Helper function that will create a well formed response for a single wombat action**
##### Params
- `action`

  A string representing the action to be taken by your wombat

- `direction`

  For `'smoke'` or `'turn'`, a string representing the direction in which to take the action. Defaults to `null`
##### Usage
```javascript
// Move forward
const resp = build_command('move')

// Turn around
const resp = build_command('turn', 'about-face')

// Throw smoke on top of yourself
const resp = build_command('smoke', 'drop')

// Shoot directly in front of yourself
const resp = build_command('shoot')

// Have your wombat actually perform the action
return {command: resp}
// Or use build_resp() to return
```
##### Returns
Returns an object containing the properties `'action'` and `'metadata'`


### `build_initial_global_state(global_size)`
**Constructs an initial global state populated by fog**
##### Params
- `global_size`

  An object with properties `height` and `width` representing the full size of the arena
##### Usage
```javascript
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()
const global_arena = build_initial_global_state(global_size)
```
##### Returns
Returns an 2 dimensional array with `height` rows where each row contains `width` shallow copies of `default_tile`


### `build_resp(command, state=null)`
**Helper function that will create a well formed response for your wombat function**
##### Params
- `command`

  The object with properties `action` and `metadata` defining the action to be taken by your wombat

- `state`

  An object representing the information you wish to save in the state object. Defaults to `null`
##### Usage
```javascript
const action = build_command('move') // However you want to construct your command

return build_resp(action)
// Or
const to_save = {did_turn: true} // Whatever you want to save in state
return build_resp(action, to_save)
```
##### Returns
Returns an object containing the properties `command` and `state`

### `can_shoot(dir, arena, arena_size, wombat={x: 3, y: 3}, wall=true)`
**Returns true if wombat shooting would damage an enemy or barrier**
##### Params
- `dir`
  
  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `arena`

  The two dimensional array of objects representing the current state of the game arena
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
- `wombat`

  The current position of the player as an object. Default coordinated are (3, 3)

- `wall`

  A boolean flag representing whether `steel-barrier`s and `wood-barrier`s should be included in determining whether you can shoot

##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()
// Direction will normally be obtained using get_direction()
let should_shoot = can_shoot('n', state.arena, global_size)

// Fetch global_arena using get_global_state()
// You can find global coordinates using from state['global-coords']
should_shoot = can_shoot('e', global_arena, global_size, wombat={x: 10, y: 4})

// You might want to prioritize shooting wombats or zakanos over wood and steel barriers
// You must provide the wombat argument as well
should_shoot = can_shoot('n', state.arena, global_size, wombat={x: 3, y: 3}, wall=false)
```

##### Returns
Returns true if there is a shootable object directly in front of wombat -- wraparound included -- or false otherwise


### `copy_arena(arena)`
**Returns a shallow copy of the arena**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena
##### Usage
```javascript
// state is the top level parameter in the wombat function
arena_copy = copy_arena(state.aren)
```
##### Returns
Returns a 2 dimensional array of objects that represents the arena

### `distance_to_tile(dir, node, arena_size, wombat={x: 3, y: 3})`
**Finds the minimum number of steps required to get from current position to node. Does not take into account actual pathfinding**
##### Params
- `dir`

  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `node`

  The target location as an object containing `x` and `y` properties representing location
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
- `wombat`

  The current position of the player as an object. Default coordinated are (3, 3)
##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()
// Direction will normally be obtained using get_direction()

let dist = distance_to_tile('n', {x: 0, y: 0}, global_size)

dist = distance_to_tile('e', {x: 12, y: 8}, global_size, {x: 10, y, 10})
```
##### Returns
Return a the minimum number of turns to get to the given position as an integer

### `filter_arena(arena, filters=null)`
**Takes in a 2 dimensional array representing the arena and returns a list of tiles of a type in `filters`**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena

- `filters`

  An array of strings where each string could be the contents of an arena tile
  Default to `null`
##### Usage
```javascript
// state is the top level parameter in the wombat function
const arena = add_locs(state.arena)

// When filters=null, return only the flattened arena without removing any contents
const all_tiles = filter_arena(arena)

const all_enemies = filter_arena(arena, ['wombat', 'zakano'])
```
##### Returns
Returns a single dimensional array of objects where the content type is in `filters`, or all tiles whe `filters` is null

### `focus_sight(arena)`
**Cuts down the 7x7 local arena to a 5x5 arena to focus on objects closer to the player**
##### Params
- `arena`

  The two dimensional (7x7) array of objects representing the current state of the game arena

##### Usage
```javascript
// state is the top level parameter in the wombat function
const arena = add_locs(state.arena)

// There are some situations where you would rather look for only tile contents within 2 block range, rather than 3
const small_view = focus_sight(arena)
```
##### Returns
Returns a two dimensional (5x5) array of objects with (2, 2) representing the center of the 7x7 array

### `front_tile(dir, arena_size, wombat={x: 3, y: 3})`
**Gets the tile directly in front of the given wombat**
###### Params
- `dir`
  
  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`

- `arena_size`

  An object with properties `height` and `width` representing the full size of the arena

- `wombat`

  The current position of the player as an object. Default coordinated are (3, 3)
##### Usage
```javascript
const arena_size = {height: 20,
              width: 20,
             }

let next_tile = front_tile('e', arena_size) 
// next_tile = {x: 4, y: 3}

next_tile = front_tile('n', arena_size, wombat={x: 3, y: 0})
// next_tile = {x: 3, y: 19}
```
##### Returns
Returns a dictions with properties `x` and `y`

### `get_arena_size(state)`
**Gets the `arena_size` object**
##### Params
- `state`

  The top level parameter of the wombat function, an object with the property `'global-dimensions'`
##### Usage
```javascript
const arena_size = get_arena_size(state)
```
##### Returns
Returns an object with the properties `height` and `width`

### `get_direction(arena)`
**Get the current direction of the player wombat**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena
##### Usage
```javascript
// state is the top level parameter in the wombat function
const current_direction = get_direction(state.arena)
```
##### Returns
Returns a strings that is one of `'n'`, `'e'`, `'s'`, or `'w'`

### `get_global_state(state, path)`
**Attempts to get the global saved state or create a new one if it isn't stored in the saved state**
##### Params
- `state`

  The top level parameter of the wombat function

- `path`

  An array of strings representing the path of properties from the top level state object to the saved global state
##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_state = get_global_state(state, ['saved-state', 'global', 'arena'])
// Update the global state and do things with it

// Use build_resp() and build_command() to simplifiy the return statement
return { 
  command: resp // Whatever action you are taking
  state: {
    global: {
      // Path matches path supplied to get_global_state()
      arena: global_state
    }
  }
}
```

### `is_clear(arena, tile)`
**Tells you whether given tile can be safely occupied or not**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena

- `tile`

  The target location as an object containing `x` and `y` properties representing location
##### Usage
```javascript
// state is the top level parameter in the wombat function
// Arbitrary tile
const front_tile = {x: 2, y: 3}

const can_move = is_clear(state.arena, front_tile)
```
##### Returns
Returns a `true` if wombat can safely occupy given tile, `false` otherwise

### `is_facing(dir, target, arena_size, wombat={x: 3, y: 3})`
**Determines if the player is currently facing target**
##### Params
- `dir`

  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `target`

  The target location as an object containing `x` and `y` properties representing location
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
- `wombat`

  The current position of the player as an object. Default coordinated are (3, 3)
##### Usage 
```javascript
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()

let facing_top_left = is_facing('n', {x: 0, y: 0}, global_size) // true
facing_top_left = is_facing('w', {x: 0, y: 0}, global_size) // true
facing_top_left = is_facing('e', {x: 0, y: 0}, global_size) // false

let facing = is_facing('s', {x: 11, y: 6}, global_size, wombat={x: 17, y: 19}) // false
```
##### Returns
Returns `true` if a move forward would bring wombat closer to target, arena wraparound included, `false` otherwise

### `merge_global_state(global_arena, local_state, global_size)`
**Add all information from local state to global state**
##### Params
- `global_arena`
  
  A two dimensional array of objects representing the global arena
- `local_state`

  The top level parameter of the wombat function
- `global_size`

  An object containing properties `height` and `width` representing the size of the global arena
##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()

const global_arena = get_global_state(state, ['saved-state', 'arena'])

const updated_global_arena = merge_global_state(global_arena, state, global_size)
```
##### Returns
Returns the global arena with each persistent tile in the local arena overwritting the corresponding tile in the global arena


### `mod(n, m)`
**A modulus function for javascript**
##### Params
- `n`

  An integer
- `m`

  An integer
##### Usage
```javascript
mod(5, 7) // 7
mod(-1, 3) // 2
mod(7, 5) // 2
```
##### Returns
Returns an integer

### `move_to(arena, arena_size, dir, loc, wombat={x: 3, y: 3})`
**Pick a move that would move you closer to target location**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
- `dir`
  
  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `loc`

  The target location as an object containing `x` and `y` properties representing location
- `wombat`

  The current position of the player as an object. Defaults to (3, 3)
##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()

// Arbitrary tiles
const enemy = {x: 5, y: 1}
const wombat = {x: 3, y: 6}

let next_move = move_to(state.arena, global_size, 'e', enemy, wombat)
// Or
next_move = move_to(state.arena, global_size, 'n', enemy)
```
##### Returns
Returns an object representing the action for the wombat to take including properties `'action'` and `'metadata'`.
Returns `null` if no way to safely move towards location

### `new_direction(direction, loc, wombat, arena_size)`
**Find which way to turn to face given location**
- `direction`
  
  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `loc`

  The target location as an object containing `x` and `y` properties representing location
- `wombat`

  The current position of the player as an object.
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
##### Usage
```javascript
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()

// Arbitrary tile
const enemy = {x: 5, y: 1}

let new_dir = new_direction('n', enemy, {x: 3, y: 1}, global_size) // 'right'
new_dir = new_direction('w', enemy, {x: 3, y: 1}, global_size) // 'about-face'
new_dir = new_direction('e', enemy, {x: 3, y: 1}, global_size) // null
```
##### Returns
Returns one of `'left'`, `'about-face'`, `'right'`, or `null`


### `possible_points(arena, wombat={x: 3, y: 3}, wall=true)`
**Get a list of all possible point sources within provided arena**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena
- `wombat`

  The current position of the player as an object.
- `wall`

  A boolean flag representing whether `steel-barrier`s and `wood-barrier`s should be included in the possible point sources
##### Usage
```javascript
// state is the top level parameter in the wombat function
const arena = state.arena

let possible_targets = possible_points(arena)
// Or
// If setting wall argument, wombat must also be provided. Use default of (3, 3) for local arena
possible_targets = possible_points(arena, wombat={x: 3, y: 3},  wall=false)

// If using the global arena instead of local arena, provide wombat location so that player is filtered out
const global_arena = state['saved-state']['arena'] // However you get your global_arena
const wombat = {x: 12, y: 7} // Get your global position from state

possible_targets = possible_points(global_arena, wombat=wombat)
// Or
possible_targets = possible_points(global_arena, wombat=wombat, wall=false)

```
##### Returns
Returns a list of objects where each object is an arena tile

### `select_target(arena, arena_size, wombat={x: 3, y: 3}, wall=true)`
**Get the closest point source to the player**
##### Params
- `arena`

  The two dimensional array of objects representing the current state of the game arena
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
- `wombat`

  The current position of the player as an object.
- `wall`

  A boolean flag representing whether `steel-barrier`s and `wood-barrier`s should be included in the possible point sources
##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()

const arena = state.arena

let target = select_target(arena, arena_size)
// Or
// If setting wall argument, wombat must also be provided. Use default of (3, 3) for local arena
target = select_target(arena, arena_size, wombat={x: 3, y: 3}, wall=false)

// If using the global arena instead of local arena, provide wombat location so that player is filtered out
const global_arena = state['saved-state']['arena'] // However you get your global_arena
const wombat = {x: 12, y: 7} // Get your global position from state

target = select_target(global_arena, arena_size, wombat=wombat)
// Or
target = select_target(global_arena, arena_size, wombat=wombat, wall=false)
```
##### Returns
Returns an object representing an arena tile with properties `'content'`, `x`, and `y`

### `shootable(dir, wombat, tile, arena_size)`
**Determines if wombat can shoot given tile**
##### Params
- `dir`
  
  The current direction the wombat is facing. One of `['n', 'e', 's', 'w']`
- `wombat`

  The current position of the player as an object.
- `tile`

  The target location as an object containing `x` and `y` properties representing location
- `arena_size`

  An object containing properties `height` and `width` representing the size of the global arena
##### Usage
```javascript
// state is the top level parameter in the wombat function
const global_size = {height: 20,
               width: 20
              }
// Global size will normally be obtained using get_arena_size()

// Arbitrary tile
const enemy = {x: 5, y: 1}
const wombat = {x: 3, y: 6}

let is_shootable = shootable('n', wombat, enemy, global_size) // false
is_shootable = shootable('e', wombat, enemy, global_size) // true
```
##### Returns
Returns `true` if tile is directly in front of wombat and within shot range, `false` otherwise


### `turn_to_dir(curr_dir, next_dir)`
**Gives the action needed to turn from `curr_dir` to `next_dir`**
##### Params
- `curr_dir`

  A string representing the current direction the wombat is facing. One of `'n'`, `'e'`, `'s'`, `'w'`
- `next_dir`

  A string representing the desired direction for the wombat to face. One of `'n'`, `'e'`, `'s'`, `'w'`

##### Usage
```javascript
let direction = turn_to_dir('s', 'n') // 'about-face'
direction = turn_to_dir('e', 'n') // 'left'
direction = turn_to_dir('w', 'n') // 'right'
direction = turn_to_dir('n', 'n') // null
```
##### Returns
Returns one of `'left'`, `'about-face'`, `'right'`, or `null`

## More Info
For more information about any of the functions or their usage, or if you'd like to use them as the starting point for any of your own wombat, view the [source code](wombat_lib.js)
