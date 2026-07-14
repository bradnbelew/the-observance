# The Observance V5 deploy marker. No story state or player progress lives in this datapack.
execute unless data storage observance:runtime version run scoreboard objectives add obs_v5 dummy
scoreboard players set runtime obs_v5 5
data modify storage observance:runtime version set value 5
