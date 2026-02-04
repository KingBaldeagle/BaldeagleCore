# Tier 1 Core Crafting

Tier 1 research cores are produced only by the Research Assembler. There are no vanilla crafting recipes.

How to craft a T1 core:

1) Place a Research Assembler block.
2) Ensure the assembler is owned by your country (it binds on placement).
3) Set the assembler tier to T1 in the GUI.
4) Insert your country’s currency into the input slot.
5) Wait for the stored Research Credits to reach 900 RC.
6) The assembler consumes the currency, outputs one T1 core, and carries any extra RC toward the next core.

Notes:
- Only currency from the assembler’s owner country is accepted.
- If the output slot is full, conversion pauses until space is available.
- Cores cannot be converted back into currency.

How a country increases RC per currency:
- RC is computed as `face value × exchange value ÷ inflation`.
- Increase exchange value by raising treasury reserves or lowering money in circulation.
- Lower inflation by burning currency (removing it from circulation) or avoiding excessive minting.
- Use higher denominations or larger stacks to contribute more RC per input.

How the modifier value is calculated:
- The GUI "Modifier" is `exchange value ÷ inflation`.
- Exchange value is based on reserves vs currency in circulation: `treasury / money in circulation`.
- More reserves or less money in circulation increases the modifier.
- Higher inflation reduces the modifier, lowering RC per unit of currency.
