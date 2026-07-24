# Relay parse examples

These Relay-style messages include the fields the local parser needs: a trip id, a positive total rate, loaded miles, and pickup/delivery address lines.

## Basic single pickup / delivery

```text
Trip ID: T-116KYL6KW
Total Rate: 2500.00
Total Loaded Miles: 850 mi
Pu-address: SWF2, Garner, NC
Del-address: TOL3, Perrysburg, OH
```

## With scheduled pickup and delivery times

```text
Trip ID: T-2026TIME01
Total Rate: $1,875.50
Total Loaded Miles: 642 mi
Pu-time: 07/24 08:00 EDT
Pu-address: CLT2, Charlotte, NC
Del-time: 07/25 14:30 EDT
Del-address: BNA3, Murfreesboro, TN
```

## Multi-stop style with alternate warehouse codes

```text
Trip ID: T-MULTI-003
Total Rate: 3200
Total Loaded Miles: 1180 mi
Pu-address: ONT8, Moreno Valley, CA
Pu-address: LAS1, North Las Vegas, NV
Del-address: DEN5, Aurora, CO
Del-address: MCI9, Liberty, MO
```
