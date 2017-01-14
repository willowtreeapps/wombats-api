# Working with WebSockets

- [Development Home](./)

### Overview

Wombats uses websockets' full-duplex communication channels to pass game information to the clients, and to enable chat to enhance the in game experience.

### Tech Overview

Since this is a Clojure project, our sockets encourage the use of EDN, however during the initial handshake, you may request sessions to transmit in JSON.

### Process

- Client sends initial connection request to the server.
- Server saves the channel id and channel to in memory state and responds to the channel with

    ```clj
    {:meta {:msg-type :handshake}
     :payload {:chan-id 1234}}
    ```

- Client then sends back to the server identifying information

    ```clj
    {:meta {:msg-type :handshake
            :chan-id 1234}
     :payload {:user-id 5678
               :game-id 4321}}
    ```

- The server now associates the appropriate user / game information with the correct channel.
- All subsequent client messages must contain the following

  ```clj
  {:meta {:msg-type MESSAGE_TYPE
          :chan-id CHANNEL_ID}
   :payload MESSAGE_PAYLOAD}
  ```

### Message Types

#### Server

**:handshake**

```clj
{:chan-id 1234}
```

Sends the channel id back to the client

**:frame-update**

```clj
{:arena [[]]}
```

Sends a new frame to the client

#### Client

**:handshake**

Sends the user information to the Server

```clj
{:user-id 1234
 :game-id 5678}
```

**:cmd**

Sends a users command choice to the Server

```clj
{:cmd :move-up}
```

###### Command list

```clj
:move-forward
:move-backwards
:turn-right
:turn-left
:smoke
:shoot
```
