CHANGELOG
=========

## Develop (6.13.2017)
**Enhancements**
* Simpler local develop setup [Eli Bosley][/elibosley] *No Issue*
* Game scheduler implemented to start a new game daily
    [Eli Bosley][/elibosley] *No Issue*
* Datomic-Pro optional based on environment [Eli Bosley][/elibosley] *No Issue*
* Simplified calls to reset, seed, etc. database [Eli Bosley][/elibosley] *No Issue*

**Bug Fixes**
* None

## QA (4.11.2017)
**Enhancements**
* None

**Bug Fixes**
* None

## Master (4.11.2017)
**Enhancements**
* AWS Lambda Mock
    [Eric Rochester][/erochest] #[329](https://github.com/willowtreeapps/wombats-api/issues/329)
* Non Arbitrary Point System
    [Matt O'Connell][/oconn] #[324](https://github.com/willowtreeapps/wombats-api/issues/324)
* Added new simulator template
    [Matt O'Connell][/oconn] *No Issue*
* Add access keys
    [Matt O'Connell][/oconn] #[325](https://github.com/willowtreeapps/wombats-api/issues/325)
* Add Eric to the DB (Seed as admin)
    [Matt O'Connell][/oconn] *No Issue*
* Add ability to update access keys
    [Matt O'Connell][/oconn] #[341](https://github.com/willowtreeapps/wombats-api/issues/341) #[332](https://github.com/willowtreeapps/wombats-api/issues/332)
* Game State Refactor
    [C.P. Dehli][/dehli], [Matt O'Connell][/oconn] #[305](https://github.com/willowtreeapps/wombats-api/issues/305)
* Zakano Code
    [Matt O'Connell][/oconn] #[330](https://github.com/willowtreeapps/wombats-api/issues/330))
* Pagination Support
    [Matt O'Connell][/oconn] #[336](https://github.com/willowtreeapps/wombats-api/issues/336)
* CORS Fixed
    [Matt O'Connell][/oconn] #[177](https://github.com/willowtreeapps/wombats-api/issues/177)

**Bug Fixes**
* Additional Error Logging in OAuth2 Response
    [Matt O'Connell][/oconn] Hotfix
* Simulator State Transfer
    [Matt O'Connell][/oconn] #[326](https://github.com/willowtreeapps/wombats-api/issues/326)
* Fixed minimap view in Simulator
    [C.P. Dehli][/dehli] #[317](https://github.com/willowtreeapps/wombats-api/issues/317)
* Access key bug
    [Matt O'Connell][/oconn] #[325](https://github.com/willowtreeapps/wombats-api/issues/325)
* Allowed origins bug
    [C.P. Dehli][/dehli] #[368](https://github.com/willowtreeapps/wombats-api/issues/368)
* Strict-Transport-Security remove includeSubdomains
    [C.P. Dehli][/dehli] *No Issue*
* Remove Swagger schema (so that Swagger site works)
    [C.P. Dehli][/dehli] *No Issue*

## 1.0.0-alpha1 (3.14.2017)
**Enhancements**
* Basic gameplay set up
* Add, edit, and delete a wombat.
* Join a Game
* Watch a game
* Chat during a game
* Playing simulator
* Game Engine
* Integration with Lambda and Datomic

**Bug Fixes**
* None

[/dehli]: https://github.com/dehli
[/emily]: https://github.com/emilyseibert
[/oconn]: https://github.com/oconn
[/erochest]: https://github.com/erochest
[/elibosley]: http://github.com/elibosley
