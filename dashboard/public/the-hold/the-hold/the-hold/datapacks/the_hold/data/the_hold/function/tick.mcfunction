execute as @a[tag=!hold_started] run function the_hold:spawn
execute as @a[tag=hold_started] unless entity @s[x=-31,y=232,z=-33,dx=62,dy=28,dz=371] run function the_hold:spawn
effect give @a[tag=hold_started] minecraft:saturation 2 0 true
execute as @a[tag=hold_started,tag=!hold_1,x=-6,y=239,z=12,dx=12,dy=8,dz=12] run function the_hold:triggers/record
execute as @a[tag=hold_1,tag=!hold_2,x=-7,y=239,z=73,dx=14,dy=8,dz=11] run function the_hold:triggers/domestic
execute as @a[tag=hold_2,tag=!hold_3,x=-7,y=238,z=137,dx=14,dy=10,dz=10] run function the_hold:triggers/cistern
execute as @a[tag=hold_3,tag=!hold_4] if block 0 240 197 minecraft:lever[powered=true] run function the_hold:triggers/lamp_change
execute as @a[tag=hold_4,tag=!hold_5,x=-5,y=239,z=253,dx=10,dy=9,dz=9] run function the_hold:triggers/register
execute as @a[tag=hold_5,tag=!hold_done,x=-7,y=239,z=316,dx=14,dy=9,dz=12] run function the_hold:triggers/final
