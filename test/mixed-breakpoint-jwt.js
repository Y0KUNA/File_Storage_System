
// mixed-breakpoint-jwt.js
//
// Phiên bản dùng JWT có sẵn.
// Không register/login.
// Mỗi VU dùng cùng một JWT được truyền qua biến môi trường.
//
// Chạy:
//   k6 run -e JWT="eyJraWQiOiIzYzgxNWYxNy05MmEwLTRhYTUtYTZjOC0wMTg4MTY3NDQyMDgiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJmaWxlLXN0b3JhZ2UtaWRlbnRpdHkiLCJzdWIiOiJlNWFiNGEwNS1hODY1LTQ1ZGUtYjUxMy1hZjk2MDVmMDE3NDkiLCJyb2xlIjoiVVNFUiIsImV4cCI6MTc5MDY5MzIyMSwiaWF0IjoxNzkwNjkyMzIxLCJ0b2tlblZlcnNpb24iOjB9.Ge4g3U9pWQmhqB7uZZ6N5S3Y-P0Nmm4AX5WgYathUJoLIuGIDJ28gr1nQ27mQjx8vwFJbEGsgWQL0_uLW4xtiU48I5Yu74hmJzNh1CpnmwOCPhY-3OmtMu9zt98Tdb3_iUR6anZ_3t6n382RJbFawG2AMhU7-qwyC2MW_8ZCwkig1ukdUMn1Isc1T-e2GQpuLlco6JAKLoSNG5z0fgBBlu1WcmGZAvJeNBhIIRyoDn_3OSd8ycPGrPEieTRGB8aMzzzw1Hdi6GvdrOZEYf-ggMkqE7T5U11tAy7CgXTWFELcAiqyRUDcteZ0RodUymhZtMYZIvij3pqGBMALqP_23g" mixed-breakpoint-jwt.js
//
// Hoặc:
//   k6 run -e JWT="$JWT" mixed-breakpoint-jwt.js
//
// Có thể chỉnh:
//   -e READERS=1000
//   -e WRITERS=500
//   -e FILE_SIZE=51200
//
// Theo dõi song song:
//   docker stats
//   CPU/RAM của máy chạy k6

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

import {
  BASE_URL,
  generateFileContent,
  headersJson,
} from './config.js';

// ---------------------------------------------------------------------------
// Tham số
// ---------------------------------------------------------------------------

const READERS = parseInt(__ENV.READERS || '1000');
const WRITERS = parseInt(__ENV.WRITERS || '500');
const FILE_SIZE = parseInt(
  __ENV.FILE_SIZE || String(50 * 1024)
);

// JWT bắt buộc phải được truyền vào.
// Có thể truyền:
//   k6 run -e JWT="..." mixed-breakpoint-jwt.js
const JWT = __ENV.JWT;

if (!JWT) {
  throw new Error(
    'Thiếu JWT. Chạy: k6 run -e JWT="your-jwt-token" mixed-breakpoint-jwt.js'
  );
}

// ---------------------------------------------------------------------------
// Metric
// ---------------------------------------------------------------------------

const rootDuration = new Trend('root_duration', true);
const rootFailed = new Counter('root_failed');
const vusReady = new Counter('vus_ready');

// ---------------------------------------------------------------------------
// Options
// ---------------------------------------------------------------------------

export const options = {
  scenarios: {
    readers: {
      executor: 'ramping-vus',
      exec: 'readerFlow',
      startVUs: 0,
      gracefulRampDown: '30s',

      stages: [
        { duration: '2m', target: Math.round(READERS * 0.2) },
        { duration: '2m', target: Math.round(READERS * 0.4) },
        { duration: '2m', target: Math.round(READERS * 0.6) },
        { duration: '2m', target: Math.round(READERS * 0.8) },
        
        { duration: '2m', target: READERS },
      ],
    },

    // writers: {
    //   executor: 'ramping-vus',
    //   exec: 'writerFlow',
    //   startVUs: 0,
    //   gracefulRampDown: '30s',

    //   stages: [
    //     { duration: '2m', target: Math.round(WRITERS * 0.17) },
    //     { duration: '2m', target: Math.round(WRITERS * 0.5) },
    //     { duration: '2m', target: Math.round(WRITERS * 0.67) },
    //     { duration: '2m', target: WRITERS },
    //   ],
    // },
  },

  thresholds: {
    // Chỉ core mới quyết định breakpoint.
    'http_req_failed{flow:core}': [
      {
        threshold: 'rate<0.05',
        abortOnFail: true,
        delayAbortEval: '30s',
      },
    ],

    'http_req_duration{flow:core}': [
      {
        threshold: 'p(95)<5000',
        abortOnFail: true,
        delayAbortEval: '30s',
      },
    ],

    // Theo dõi từng endpoint.
    'http_req_duration{name:list_children}': [
      'p(95)<2000',
    ],

    'http_req_duration{name:get_quota}': [
      'p(95)<2000',
    ],

    'http_req_duration{name:upload_init}': [
      'p(95)<3000',
    ],

    'http_req_duration{name:minio_put}': [
      'p(95)<3000',
    ],

    'http_req_duration{name:upload_confirm}': [
      'p(95)<3000',
    ],

    // Root không phải workload chính.
    'http_req_duration{name:auth_root}': [
      'p(95)<5000',
    ],
  },
};

// ---------------------------------------------------------------------------
// Authentication context
//
// Không register/login.
// Mỗi VU chỉ dùng JWT được truyền vào.
// ---------------------------------------------------------------------------

function createCtx() {
  return {
    authHeaders: {
      ...headersJson,
      Authorization: `Bearer ${JWT}`,
    },

    rootFolderId: null,
    ready: false,
  };
}

// ---------------------------------------------------------------------------
// Lấy root folder một lần cho mỗi VU.
//
// /folders/root vẫn được gọi vì reader/writer cần rootFolderId.
// Đây không phải authentication.
//
// Nếu root đã biết trước, có thể bỏ hoàn toàn request này và truyền
// ROOT_FOLDER_ID qua biến môi trường.
// ---------------------------------------------------------------------------

function ensureReady(ctx) {
  if (ctx.ready) {
    return true;
  }

  const res = http.get(`${BASE_URL}/folders/root`, {
    headers: ctx.authHeaders,

    tags: {
      name: 'auth_root',
      flow: 'setup',
    },
  });

  rootDuration.add(res.timings.duration);

  if (res.status !== 200) {
    rootFailed.add(1);
    return false;
  }

  try {
    ctx.rootFolderId = JSON.parse(res.body).id;
  } catch (e) {
    rootFailed.add(1);
    return false;
  }

  ctx.ready = true;
  vusReady.add(1);

  return true;
}

// ---------------------------------------------------------------------------
// Mỗi scenario có context riêng cho từng VU.
// ---------------------------------------------------------------------------

let readerCtx = null;
let writerCtx = null;

// ---------------------------------------------------------------------------
// Reader
// ---------------------------------------------------------------------------

export function readerFlow() {
  if (!readerCtx) {
    readerCtx = createCtx();
  }

  if (!ensureReady(readerCtx)) {
    sleep(2);
    return;
  }

  const {
    authHeaders,
    rootFolderId,
  } = readerCtx;

  // -------------------------------------------------------------------------
  // List children
  // -------------------------------------------------------------------------

  const childrenRes = http.get(
    `${BASE_URL}/folders/${rootFolderId}/children`,
    {
      headers: authHeaders,

      tags: {
        name: 'list_children',
        flow: 'core',
      },
    }
  );

  check(childrenRes, {
    'list children: 200': (r) => r.status === 200,
  });

  // -------------------------------------------------------------------------
  // Get quota
  // -------------------------------------------------------------------------

  const quotaRes = http.get(
    `${BASE_URL}/quota`,
    {
      headers: authHeaders,

      tags: {
        name: 'get_quota',
        flow: 'core',
      },
    }
  );

  check(quotaRes, {
    'quota: 200': (r) => r.status === 200,
  });

  sleep(Math.random() * 2 + 1);
}

// ---------------------------------------------------------------------------
// Writer
// ---------------------------------------------------------------------------

// export function writerFlow() {
//   if (!writerCtx) {
//     writerCtx = createCtx();
//   }

//   if (!ensureReady(writerCtx)) {
//     sleep(2);
//     return;
//   }

//   const {
//     authHeaders,
//     rootFolderId,
//   } = writerCtx;

//   // -------------------------------------------------------------------------
//   // Upload init
//   // -------------------------------------------------------------------------

//   const initRes = http.post(
//     `${BASE_URL}/uploads`,

//     JSON.stringify({
//       parentFolderId: rootFolderId,

//       name:
//         `bp-${__VU}-${__ITER}-${Date.now()}.txt`,

//       size: FILE_SIZE,

//       mimeType: 'text/plain',
//     }),

//     {
//       headers: authHeaders,

//       tags: {
//         name: 'upload_init',
//         flow: 'core',
//       },
//     }
//   );

//   const initOk = check(initRes, {
//     'upload init: 201': (r) => r.status === 201,
//   });

//   if (!initOk) {
//     sleep(Math.random() * 3 + 2);
//     return;
//   }

//   let initBody;

//   try {
//     initBody = JSON.parse(initRes.body);
//   } catch (e) {
//     sleep(Math.random() * 3 + 2);
//     return;
//   }

//   // -------------------------------------------------------------------------
//   // PUT trực tiếp vào MinIO
//   // -------------------------------------------------------------------------

//   const putRes = http.put(
//     initBody.uploadUrl,

//     generateFileContent(FILE_SIZE),

//     {
//       headers: {
//         'Content-Type': 'text/plain',
//       },

//       tags: {
//         name: 'minio_put',
//         flow: 'core',
//       },
//     }
//   );

//   const putOk = check(putRes, {
//     'MinIO PUT: 200': (r) => r.status === 200,
//   });

//   // -------------------------------------------------------------------------
//   // Chỉ confirm nếu PUT thành công
//   // -------------------------------------------------------------------------

//   if (putOk) {
//     const confirmRes = http.post(
//       `${BASE_URL}/uploads/${initBody.fileId}/confirm`,

//       JSON.stringify({}),

//       {
//         headers: authHeaders,

//         tags: {
//           name: 'upload_confirm',
//           flow: 'core',
//         },
//       }
//     );

//     check(confirmRes, {
//       'confirm: 2xx': (r) =>
//         r.status >= 200 && r.status < 300,
//     });
//   }

//   sleep(Math.random() * 3 + 2);
// }

