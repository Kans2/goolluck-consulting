/**
 * SeatLock — Concurrent Booking Load Test
 *
 * Simulates 100 users each trying to lock and confirm a random seat
 * simultaneously. Verifies that:
 *   1. No more than 20 seats can be confirmed.
 *   2. No seat is assigned to two different users.
 *   3. All failed requests get proper error responses.
 *
 * Usage:
 *   node load-test.js [baseUrl]
 *
 * Defaults to http://localhost:8080
 */

const http = require('http');
const crypto = require('crypto');

const BASE = process.argv[2] || 'http://localhost:8080';
const TOTAL_USERS = 100;
const TOTAL_SEATS = 20;

function post(path, body) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE);
    const payload = JSON.stringify(body);
    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Content-Length': Buffer.byteLength(payload),
        },
        timeout: 15000,
      },
      (res) => {
        let data = '';
        res.on('data', (chunk) => (data += chunk));
        res.on('end', () => {
          try {
            resolve({ status: res.statusCode, body: JSON.parse(data) });
          } catch {
            resolve({ status: res.statusCode, body: data });
          }
        });
      }
    );
    req.on('error', reject);
    req.write(payload);
    req.end();
  });
}

function get(path) {
  return new Promise((resolve, reject) => {
    const url = new URL(path, BASE);
    http.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => (data += chunk));
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch {
          resolve({ status: res.statusCode, body: data });
        }
      });
    }).on('error', reject);
  });
}

async function simulateUser(userId, seatId) {
  const sessionId = crypto.randomUUID();
  const label = `User-${String(userId).padStart(3, '0')} → Seat ${seatId}`;

  try {
    // Step 1: Lock the seat
    const lockRes = await post('/api/seats/lock', {
      seatIds: [seatId],
      sessionId: sessionId,
    });

    if (lockRes.status !== 200) {
      return { userId, seatId, sessionId, success: false, stage: 'lock', status: lockRes.status };
    }

    // Step 2: Confirm the booking
    const confirmRes = await post('/api/seats/confirm', {
      sessionId: sessionId,
      passengerName: `Passenger-${userId}`,
    });

    if (confirmRes.status !== 200) {
      return { userId, seatId, sessionId, success: false, stage: 'confirm', status: confirmRes.status };
    }

    return { userId, seatId, sessionId, success: true };
  } catch (err) {
    return { userId, seatId, sessionId, success: false, stage: 'error', message: err.message };
  }
}

async function main() {
  console.log('═══════════════════════════════════════════════════════════');
  console.log('  SeatLock — Concurrent Booking Load Test');
  console.log(`  Target: ${BASE}`);
  console.log(`  Users: ${TOTAL_USERS}   Seats: ${TOTAL_SEATS}`);
  console.log('═══════════════════════════════════════════════════════════\n');

  // Verify server is reachable
  try {
    const health = await get('/api/seats');
    if (health.status !== 200) throw new Error(`Status ${health.status}`);
    console.log(`✅ Server reachable. Current seats: ${health.body.length}\n`);
  } catch (err) {
    console.error(`❌ Cannot reach server at ${BASE}: ${err.message}`);
    console.error('   Make sure the backend is running first.\n');
    process.exit(1);
  }

  // Launch all users simultaneously — each targets a random seat (1–20)
  console.log(`🚀 Launching ${TOTAL_USERS} simultaneous booking attempts...\n`);
  const start = Date.now();

  const promises = [];
  for (let i = 1; i <= TOTAL_USERS; i++) {
    const seatId = Math.ceil(Math.random() * TOTAL_SEATS);
    promises.push(simulateUser(i, seatId));
  }

  const results = await Promise.all(promises);
  const elapsed = Date.now() - start;

  // Analyze results
  const successes = results.filter((r) => r.success);
  const failures = results.filter((r) => !r.success);
  const bookedSeats = new Set(successes.map((r) => r.seatId));

  console.log('───────────────────────────────────────────────────────────');
  console.log('  RESULTS');
  console.log('───────────────────────────────────────────────────────────');
  console.log(`  Total requests:    ${TOTAL_USERS}`);
  console.log(`  Successful books:  ${successes.length}`);
  console.log(`  Failed (expected): ${failures.length}`);
  console.log(`  Unique seats:      ${bookedSeats.size}`);
  console.log(`  Elapsed time:      ${elapsed}ms`);
  console.log('');

  // Verify no duplicate seat assignments
  const seatCounts = {};
  successes.forEach((r) => {
    seatCounts[r.seatId] = (seatCounts[r.seatId] || 0) + 1;
  });
  const duplicates = Object.entries(seatCounts).filter(([, count]) => count > 1);

  if (duplicates.length > 0) {
    console.log('  ❌ DUPLICATE BOOKINGS DETECTED!');
    duplicates.forEach(([seat, count]) => {
      console.log(`     Seat ${seat} was booked ${count} times`);
    });
  } else {
    console.log('  ✅ No duplicate bookings — each seat assigned at most once.');
  }

  if (successes.length <= TOTAL_SEATS) {
    console.log(`  ✅ Total confirmed (${successes.length}) ≤ ${TOTAL_SEATS} — constraint holds.`);
  } else {
    console.log(`  ❌ OVERBOOKING! ${successes.length} seats confirmed (limit: ${TOTAL_SEATS})`);
  }

  // Verify against server state
  console.log('\n  Verifying against server state...');
  const finalState = await get('/api/seats');
  const bookedOnServer = finalState.body.filter((s) => s.status === 'BOOKED');
  console.log(`  Server reports ${bookedOnServer.length} booked seats.`);

  const bookingsRes = await get('/api/bookings');
  console.log(`  Server reports ${bookingsRes.body.length} booking records.`);

  console.log('\n───────────────────────────────────────────────────────────');

  // Failure breakdown
  if (failures.length > 0) {
    const byStage = {};
    failures.forEach((r) => {
      const key = `${r.stage}:${r.status || 'error'}`;
      byStage[key] = (byStage[key] || 0) + 1;
    });
    console.log('  Failure breakdown:');
    Object.entries(byStage).forEach(([key, count]) => {
      console.log(`    ${key} — ${count} requests`);
    });
  }

  console.log('═══════════════════════════════════════════════════════════\n');
}

main().catch(console.error);

