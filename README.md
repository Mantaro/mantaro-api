## Mantaro's API
This handles all of [Mantaro's](https://github.com/Mantaro/MantaroBot) games and works as a bridge between the different instances of the bot to get shared data

Usually this includes
- Splashes
- Pokemon Game
- Character Game
- "Hush" / Secret Achievements
- Patreon handling (Receive pledge data, refresh pledges)


The rest of the inter-node and inter-bot communication is done in Redis.


### Game data 
The data for the Pokemon and Character game is not shared here and is not gonna be made public as this leads to userbot abuse (say, match the image url to a pokemon and answer automatically). Don't ask for it.

To be fair, it's just a text file with the following, repeated a few thousand times:
```
imageurl,name
```


## Legal Stuff
Copyright (C) 2016-2020 **David Rubio Escares** / **Kodehawa**

```
This program is free software: you can redistribute it and/or modify it under the terms of the 
GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version. 
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU General Public License for more details. 
You should have received a copy of the GNU General Public License along with this program. 
If not, see http://www.gnu.org/licenses/
```  

[The full license can be found here.](https://github.com/Kodehawa/mantaro-api/blob/master/LICENSE)
