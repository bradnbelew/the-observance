tag @s add hold_4
fill -2 241 184 2 241 189 minecraft:campfire[lit=false]
setblock 0 244 187 minecraft:oxidized_copper_bulb[lit=true,powered=true]
setblock 0 243 187 minecraft:light[level=12]
setblock 0 242 199 minecraft:dark_oak_wall_sign[facing=north]{front_text:{messages:[{text:"LAMP RETURNED",color:"gray"},{text:"DISPATCH IV",color:"gray"},{text:"Z",color:"gray"},{text:"FILED",color:"gray"}]}}
fill -2 240 207 2 246 207 minecraft:air
title @s actionbar {text:"the untended lamp wakes. the kept fire dies.",color:"dark_aqua"}
playsound minecraft:block.respawn_anchor.deplete master @s 0 242 187 0.75 0.55
playsound minecraft:block.vault.open_shutter master @s 0 242 207 0.6 0.65
