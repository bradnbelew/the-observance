tag @s remove hold_1
tag @s remove hold_2
tag @s remove hold_3
tag @s remove hold_4
tag @s remove hold_5
tag @s remove hold_done
clear @s
effect clear @s
gamemode adventure @s
tp @s 0.5 240 -23.5 0 0
spawnpoint @s 0 240 -23
tag @s add hold_started
title @s times 20 80 40
title @s subtitle {text:"follow the lit aisle. read what was left.",color:"dark_gray"}
title @s title {text:"THE HOLD",color:"gray",bold:true}
playsound minecraft:ambient.cave master @s 0 241 -18 0.35 0.65
