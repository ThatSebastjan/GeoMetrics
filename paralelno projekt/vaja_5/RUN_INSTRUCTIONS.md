# Navodila za Zagon (Run Instructions)

## Pregled

Ta aplikacija omogoca porazdeljeno rudarjenje blokov z uporabo MPI (Message Passing Interface) in TCP komunikacije med vozlisci. Vsako vozlisce rudari bloke z uporabo vseh razpolozljivih procesorskih jeder.

---

## Predpogoji

1. **MPI knjiznica** mora biti namescena:
   - **Windows**: MS-MPI ali Intel MPI
   - **Linux**: OpenMPI (`sudo apt-get install libopenmpi-dev`)

2. **Prevedena aplikacija**: `vaja_5.exe` (Windows) ali `vaja_5` (Linux)

3. **Odprt port**: 
   - Brez MPI: Aplikacija uporablja porte zacenjajoc s 1234
   - Z MPI: Vsak rank uporablja svoj port (rank 0 = 1234, rank 1 = 1244, rank 2 = 1254, itd.)

---

## Nacin 1: MPI Porazdeljeno Rudarjenje (Priporoceno)

### Windows (MS-MPI)

```bash
# Zazeni 2 instanci z MPI
mpiexec -n 2 vaja_5.exe

# Ali 4 instance
mpiexec -n 4 vaja_5.exe
```

### Linux (OpenMPI)

```bash
# Zazeni 2 instanci z MPI
mpirun -np 2 ./vaja_5

# Ali 4 instance
mpirun -np 4 ./vaja_5

# Z dolocenimi gostitelji
mpirun -np 4 -hostfile hosts.txt ./vaja_5
```

### Kaj se zgodi?

1. Vsako MPI vozlisce dobi svoj rank (0, 1, 2, ...)
2. Vsako vozlisce zazene TCP streznik na svojem portu (rank 0 = port 1234, rank 1 = port 1244, ...)
3. Vozlisca si samodejno izmenjujejo bloke preko MPI
4. Lokalno paralelno rudarjenje (vse procesorske niti)
5. Avtomatska sinhronizacija verig med MPI vozlisci

---

## Nacin 2: TCP Povezava (Brez MPI)

### Korak 1: Zazeni prvo instanco

```bash
# Windows
.\vaja_5.exe

# Linux
./vaja_5
```

Opomba: Zapomni si **port** (npr. 1234), ki se izpise v konzoli:
```
Local node listening on port 1234
```

### Korak 2: Zazeni drugo instanco

```bash
# Zazeni v novem terminalnem oknu
.\vaja_5.exe
```

Ta instanca bo dobila drug port (npr. 1235).

### Korak 3: Povezi vozlisca preko GUI

1. V **drugi instanci** vnesi port **prve instance** (1234)
2. Klikni **"Connect"**
3. Vozlisci sta povezani in si izmenjujeta bloke

### Korak 4: Zazeni rudarjenje

1. V obeh oknih klikni gumb **"Mine"**
2. Obe instanci zacneta rudariti bloke
3. Bloki se samodejno sinhronizirajo preko TCP

---

## Nacin 3: MPI + TCP Kombinacija

### Zazeni MPI vozlisca

```bash
mpiexec -n 2 vaga_5.exe
```

### Dodatno povezi tretjo instanco preko TCP

1. Zazeni tretjo instanco brez MPI:
   ```bash
   .\vaja_5.exe
   ```

2. V GUI tretje instance vnesi port ene od MPI instanc (npr. 1234 za rank 0 ali 1244 za rank 1)

3. Klikni **"Connect"**

Zdaj imas:
- 2 MPI vozlisci (komunikacija preko MPI + TCP)
- 1 TCP vozlisce (komunikacija samo preko TCP)
- Vsi si izmenjujejo bloke in sinhronizirajo verige!

---

## Tipicni Delovni Tok

### Scenario 1: 2 MPI Vozlisci

```bash
# Terminal
mpiexec -n 2 vaja_5.exe

# V GUI obeh instanc
1. Klikni "Mine" na obeh vozliscih
2. Opazuj, kako se bloki dodajajo v verigo
3. Opazuj avtomatsko sinhronizacijo med vozlisci
```

### Scenario 2: 2 TCP Vozlisci

```bash
# Terminal 1
.\vaja_5.exe
# Opomba: Port 1234

# Terminal 2
.\vaja_5.exe
# Opomba: Port 1235

# V GUI instance 2
1. Vnesi "1234" v "Remote port"
2. Klikni "Connect"
3. Klikni "Mine" na obeh instancah
4. Opazuj sinhronizacijo
```

### Scenario 3: 4 MPI + 1 TCP

```bash
# Terminal 1
mpiexec -n 4 vaja_5.exe

# Terminal 2 (nova instanca)
.\vaja_5.exe
# V GUI: Connect na port 1234
```

---

## Prilagoditve Konfiguracije

### Spremeni zacetno tezavnost

V `blockchain.cpp`:
```cpp
int current_difficulty = 4;  // Spremeni na 3 za hitrejse rudarjenje
```

### Spremeni interval prilagoditve tezavnosti

V `blockchain.h`:
```cpp
constexpr int BLOCK_GEN_INTERVAL = 10;       // sekunde
constexpr int DIFFICULTY_ADJUST_INTERVAL = 10;  // stevilo blokov
```

### Spremeni zacetni TCP port

V `networking.h`:
```cpp
constexpr int SERVER_START_PORT = 1234;  // Spremeni na drug port
```

**Opomba**: Pri MPI se vsak rank offseta za 10 portov (rank 0 = 1234, rank 1 = 1244, itd.)

---

## Struktura Projekta

```
vaja_5/
|-- main.cpp              # Vstopna tocka
|-- mining.cpp/h          # Paralelno rudarjenje
|-- blockchain.cpp/h      # Veriga blokov
|-- networking.cpp/h      # TCP komunikacija
|-- mpi_networking.cpp/h  # MPI komunikacija
|-- rendering.cpp/h       # GUI (ImGui)
|-- block.h               # Struktura bloka
```

---

## Kontrolni Seznam

- [ ] MPI namescen in funkcionalen
- [ ] Aplikacija prevedena brez napak
- [ ] Zazeni 2 instanci z MPI
- [ ] Klikni "Mine" in opazuj bloke
- [ ] Zazeni 2 instanci brez MPI
- [ ] Povezi ju preko TCP
- [ ] Preveri sinhronizacijo blokov
- [ ] Testiraj konflikt verig

---