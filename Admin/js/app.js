const apps = [
  {
    id: 1,
    n: 'Lerato Mokoena',
    no: 'STU00123',
    course: 'BSc Computer Science',
    date: '24 Sep 2026',
    status: 'Pending'
  },
  {
    id: 2,
    n: 'Sipho Dlamini',
    no: 'STU00124',
    course: 'BCom Accounting',
    date: '24 Sep 2026',
    status: 'Pending'
  },
  {
    id: 3,
    n: 'Thando Ndlovu',
    no: 'STU00125',
    course: 'BEd',
    date: '23 Sep 2026',
    status: 'Approved'
  },
  {
    id: 4,
    n: 'Naledi Khumalo',
    no: 'STU00126',
    course: 'BA',
    date: '23 Sep 2026',
    status: 'Pending'
  },
  {
    id: 5,
    n: 'Jason Mthembu',
    no: 'STU00127',
    course: 'BSc Engineering',
    date: '22 Sep 2026',
    status: 'Pending'
  }
];

const students = [
  ['Thando Ndlovu', 'STU00125', 'BEd', 'Unallocated']
];

const rooms = [
  ['A101', 'Single', 1, 0],
  ['A102', 'Shared', 2, 0],
  ['A103', 'Shared', 2, 0],
  ['A104', 'Shared', 4, 0],
  ['B01', 'Single', 1, 0]
];


/* =========================================================
   ALLOCATIONS
   ========================================================= */

const allocations = [];


/* =========================================================
   PAYMENTS
   ========================================================= */

const payments = [];


/* =========================================================
   MESSAGES
   ========================================================= */

const messages = [];


/* =========================================================
   MAINTENANCE
   ========================================================= */

const maintenance = [
  ['Leaking tap', 'A101', 'Plumbing', 'Open'],
  ['Broken light', 'A103', 'Electrical', 'In Progress'],
  ['Door handle', 'B01', 'General', 'Resolved']
];


/* =========================================================
   GENERAL HELPERS
   ========================================================= */

function esc(value) {
  return String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

function toast(message) {
  const element = document.getElementById('toast');

  if (!element) {
    return;
  }

  element.textContent = message;
  element.classList.add('show');

  clearTimeout(window.toastTimer);

  window.toastTimer = setTimeout(() => {
    element.classList.remove('show');
  }, 3000);
}

function sc(status) {
  const value = String(status || '').toLowerCase();

  if (
    value === 'approved' ||
    value === 'paid' ||
    value === 'active' ||
    value === 'resolved' ||
    value === 'allocated' ||
    value === 'available'
  ) {
    return 'active';
  }

  if (
    value === 'pending' ||
    value === 'in progress' ||
    value === 'not full' ||
    value === 'unallocated'
  ) {
    return 'pending';
  }

  if (
    value === 'rejected' ||
    value === 'open' ||
    value === 'full'
  ) {
    return 'danger';
  }

  return '';
}

function cell(name, studentNumber) {
  const initials = String(name || '')
    .split(' ')
    .map(part => part.charAt(0))
    .join('')
    .substring(0, 2)
    .toUpperCase();

  return `
    <div class="usercell">
      <div class="avatar">${esc(initials)}</div>

      <div>
        <strong>${esc(name)}</strong>
        <small>${esc(studentNumber)}</small>
      </div>
    </div>
  `;
}

function table(headers, rows) {
  return `
    <div class="tablewrap">
      <table class="table">
        <thead>
          <tr>
            ${headers.map(header => `<th>${header}</th>`).join('')}
          </tr>
        </thead>

        <tbody>
          ${rows.join('')}
        </tbody>
      </table>
    </div>
  `;
}

function head(title, subtitle, button = '') {
  return `
    <div class="pagehead">
      <div>
        <h2>${esc(title)}</h2>
        <p>${esc(subtitle)}</p>
      </div>

      ${button}
    </div>
  `;
}

function page(content) {
  const pageElement = document.getElementById('page');

  if (pageElement) {
    pageElement.innerHTML = content;
  }
}

window.showPage = page;


/* =========================================================
   STUDENT HELPERS
   ========================================================= */

function findStudent(studentNumber) {
  return students.find(
    student => student[1] === studentNumber
  );
}

function findStudentByNumber(studentNumber) {
  return students.find(
    student => student[1] === studentNumber
  );
}


/* =========================================================
   ROOM HELPERS
   ========================================================= */

function findRoom(roomNumber) {
  return rooms.find(
    room => room[0] === roomNumber
  );
}


/* =========================================================
   ALLOCATION HELPERS
   ========================================================= */

function findStudentAllocation(studentNumber) {
  return allocations.find(
    allocation =>
      allocation.studentNumber === studentNumber
  );
}

function getRoomAllocations(roomNumber) {
  return allocations.filter(
    allocation =>
      allocation.roomNumber === roomNumber
  );
}

function getOccupiedBeds(roomNumber) {
  return getRoomAllocations(roomNumber).length;
}

function getAvailableBeds(roomNumber) {
  const room = findRoom(roomNumber);

  if (!room) {
    return 0;
  }

  return Math.max(
    0,
    room[2] - getOccupiedBeds(roomNumber)
  );
}

function getRoomStatus(roomNumber) {
  const room = findRoom(roomNumber);

  if (!room) {
    return 'Unknown';
  }

  const occupied = getOccupiedBeds(roomNumber);
  const total = room[2];

  if (occupied >= total) {
    return 'Full';
  }

  if (occupied === 0) {
    return 'Available';
  }

  return 'Not Full';
}

function getNextAvailableBed(roomNumber) {
  const room = findRoom(roomNumber);

  if (!room) {
    return null;
  }

  const usedBeds = getRoomAllocations(roomNumber)
    .map(allocation => Number(allocation.bedNumber))
    .filter(number => Number.isInteger(number));

  for (
    let bedNumber = 1;
    bedNumber <= room[2];
    bedNumber++
  ) {
    if (!usedBeds.includes(bedNumber)) {
      return bedNumber;
    }
  }

  return null;
}

function syncStudentRooms() {
  students.forEach(student => {
    const allocation =
      findStudentAllocation(student[1]);

    if (allocation) {
      student[3] = allocation.roomNumber;
    } else {
      student[3] = 'Unallocated';
    }
  });
}


/* =========================================================
   PAYMENT HELPERS
   ========================================================= */

function getStudentPayment(studentNumber) {
  return payments.find(
    payment =>
      payment.studentNumber === studentNumber
  );
}

function createPaymentForStudent(student) {
  if (!student) {
    return;
  }

  const existingPayment =
    getStudentPayment(student[1]);

  if (existingPayment) {
    return;
  }

  payments.push({
    studentNumber: student[1],
    studentName: student[0],
    amount: 'R 2,500',
    status: 'Pending'
  });
}


/* =========================================================
   MESSAGE HELPERS
   ========================================================= */

function getStudentMessages(studentNumber) {
  return messages.filter(
    message =>
      message.studentNumber === studentNumber
  );
}

function createWelcomeMessage(student) {
  if (!student) {
    return;
  }

  const alreadyExists = messages.some(
    message =>
      message.studentNumber === student[1] &&
      message.subject === 'Residence Registration'
  );

  if (alreadyExists) {
    return;
  }

  messages.push({
    studentNumber: student[1],
    studentName: student[0],
    subject: 'Residence Registration',
    message:
      'Your residence registration has been completed. Your room allocation will appear once a room has been assigned.',
    date: 'Today'
  });
}


/* =========================================================
   MAINTENANCE HELPERS
   ========================================================= */

function getStudentsInRoom(roomNumber) {
  return students.filter(student => {
    const allocation =
      findStudentAllocation(student[1]);

    return (
      allocation &&
      allocation.roomNumber === roomNumber
    );
  });
}

function createMaintenanceMessage(
  maintenanceItem,
  status
) {
  const issue = maintenanceItem[0];
  const roomNumber = maintenanceItem[1];

  const studentsInRoom =
    getStudentsInRoom(roomNumber);

  studentsInRoom.forEach(student => {

    const subject = 'Maintenance Update';

    const alreadyExists = messages.some(
      message =>
        message.studentNumber === student[1] &&
        message.subject === subject &&
        message.message.includes(issue) &&
        message.message.includes(status)
    );

    if (alreadyExists) {
      return;
    }

    messages.push({
      studentNumber: student[1],
      studentName: student[0],
      subject,
      message:
        'Maintenance update for ' +
        roomNumber +
        ': ' +
        issue +
        ' is now marked as ' +
        status +
        '.',
      date: 'Today'
    });
  });
}

function getMaintenanceAffectedStudents(roomNumber) {
  return getStudentsInRoom(roomNumber);
}


/* =========================================================
   PAGES
   ========================================================= */

const pages = {

  /* =======================================================
     DASHBOARD
     ======================================================= */

  dashboard() {
    syncStudentRooms();

    const pending =
      apps.filter(
        app => app.status === 'Pending'
      ).length;

    const approved =
      apps.filter(
        app => app.status === 'Approved'
      ).length;

    const totalBeds =
      rooms.reduce(
        (total, room) => total + room[2],
        0
      );

    const occupiedBeds =
      allocations.length;

    const availableBeds =
      Math.max(
        0,
        totalBeds - occupiedBeds
      );

    return `
      ${head(
        'Dashboard',
        'Overview of Luhambo Residence'
      )}

      <div class="stats">

        <div class="stat">
          <div class="staticon">📄</div>

          <div>
            <strong>${apps.length}</strong>
            <span>Total Applications</span>
          </div>
        </div>

        <div class="stat">
          <div class="staticon">⏳</div>

          <div>
            <strong>${pending}</strong>
            <span>Pending Applications</span>
          </div>
        </div>

        <div class="stat">
          <div class="staticon">👨‍🎓</div>

          <div>
            <strong>${students.length}</strong>
            <span>Registered Students</span>
          </div>
        </div>

        <div class="stat">
          <div class="staticon">🛏</div>

          <div>
            <strong>${occupiedBeds}/${totalBeds}</strong>
            <span>Occupied Beds</span>
          </div>
        </div>

      </div>

      <div class="grid2">

        <div class="card">

          <div class="cardhead">
            <div>
              <h3>Residence Occupancy</h3>
              <p>Current room and bed usage</p>
            </div>
          </div>

          <div class="occupancy">

            <div class="occupancyrow">
              <span>Occupied</span>
              <strong>
                ${occupiedBeds} / ${totalBeds}
              </strong>
            </div>

            <div class="progress">
              <span style="width: ${
                totalBeds
                  ? Math.round(
                      (occupiedBeds / totalBeds) * 100
                    )
                  : 0
              }%"></span>
            </div>

            <div class="occupancyrow">
              <span>Available</span>
              <strong>${availableBeds}</strong>
            </div>

          </div>

        </div>

        <div class="card">

          <div class="cardhead">
            <div>
              <h3>Application Summary</h3>
              <p>Current application statuses</p>
            </div>
          </div>

          <div class="summarylist">

            <div>
              <span>Pending</span>
              <strong>${pending}</strong>
            </div>

            <div>
              <span>Approved</span>
              <strong>${approved}</strong>
            </div>

            <div>
              <span>Total</span>
              <strong>${apps.length}</strong>
            </div>

          </div>

        </div>

      </div>
    `;
  },


  /* =======================================================
     APPLICATIONS
     ======================================================= */

  applications() {
    return `
      ${head(
        'Applications',
        'Review and manage student accommodation applications'
      )}

      ${table(
        [
          'Student',
          'Course',
          'Date',
          'Status',
          'Action'
        ],

        apps.map(app => `
          <tr>

            <td>
              ${cell(app.n, app.no)}
            </td>

            <td>
              ${esc(app.course)}
            </td>

            <td>
              ${esc(app.date)}
            </td>

            <td>
              <span class="status ${sc(app.status)}">
                ${esc(app.status)}
              </span>
            </td>

            <td>

              ${
                app.status === 'Pending'
                  ? `
                    <button
                      class="btn blue"
                      onclick="decision(${app.id}, 'Approved')"
                    >
                      Approve
                    </button>

                    <button
                      class="btn light"
                      onclick="decision(${app.id}, 'Rejected')"
                    >
                      Reject
                    </button>
                  `
                  : `
                    <button
                      class="btn light"
                      onclick="viewApplication(${app.id})"
                    >
                      View
                    </button>
                  `
              }

            </td>

          </tr>
        `)
      )}
    `;
  },


  /* =======================================================
     STUDENTS
     ======================================================= */

  students() {
    syncStudentRooms();

    return `
      ${head(
        'Students',
        'Manage registered students',
        `
          <button
            class="btn blue"
            onclick="openStudent()"
          >
            + Add Student
          </button>
        `
      )}

      ${table(
        [
          'Student',
          'Course',
          'Room',
          'Status',
          'Action'
        ],

        students.map(student => {

          const allocation =
            findStudentAllocation(student[1]);

          const room =
            allocation
              ? allocation.roomNumber
              : 'Unallocated';

          return `
            <tr>

              <td>
                ${cell(student[0], student[1])}
              </td>

              <td>
                ${esc(student[2])}
              </td>

              <td>
                ${esc(room)}
              </td>

              <td>
                <span class="status ${
                  allocation
                    ? 'active'
                    : 'pending'
                }">
                  ${
                    allocation
                      ? 'Allocated'
                      : 'Unallocated'
                  }
                </span>
              </td>

              <td>
                <button
                  class="btn light"
                  onclick="openStudent('${esc(student[1])}')"
                >
                  View
                </button>
              </td>

            </tr>
          `;
        })
      )}
    `;
  },


  /* =======================================================
     ROOMS
     ======================================================= */

  rooms() {
    return `
      ${head(
        'Rooms & Beds',
        'Manage residence rooms and bed availability',
        `
          <button
            class="btn blue"
            onclick="openRoom()"
          >
            + Add Room
          </button>
        `
      )}

      ${table(
        [
          'Room',
          'Type',
          'Beds',
          'Occupied',
          'Available',
          'Status',
          'Action'
        ],

        rooms.map(room => {

          const roomNumber = room[0];
          const roomType = room[1];
          const totalBeds = room[2];

          const occupied =
            getOccupiedBeds(roomNumber);

          const available =
            getAvailableBeds(roomNumber);

          const status =
            getRoomStatus(roomNumber);

          return `
            <tr>

              <td>
                <strong>
                  ${esc(roomNumber)}
                </strong>
              </td>

              <td>
                ${esc(roomType)}
              </td>

              <td>
                ${totalBeds}
              </td>

              <td>
                ${occupied}/${totalBeds}
              </td>

              <td>
                ${available}
              </td>

              <td>
                <span class="status ${sc(status)}">
                  ${esc(status)}
                </span>
              </td>

              <td>
                <button
                  class="btn light"
                  onclick="editRoom('${esc(roomNumber)}')"
                >
                  Edit
                </button>
              </td>

            </tr>
          `;
        })
      )}
    `;
  },


  /* =======================================================
     ALLOCATIONS
     ======================================================= */

  allocations() {
    syncStudentRooms();

    return `
      ${head(
        'Room Allocation',
        'Allocate students to available rooms and beds',
        `
          <button
            class="btn blue"
            onclick="openAllocation()"
          >
            + Allocate Student
          </button>
        `
      )}

      ${table(
        [
          'Student',
          'Room',
          'Bed',
          'Room Occupancy',
          'Availability',
          'Action'
        ],

        students.map(student => {

          const allocation =
            findStudentAllocation(student[1]);

          if (!allocation) {
            return `
              <tr>

                <td>
                  ${cell(student[0], student[1])}
                </td>

                <td>
                  <span>
                    Unallocated
                  </span>
                </td>

                <td>
                  -
                </td>

                <td>
                  -
                </td>

                <td>
                  <span class="status pending">
                    Unallocated
                  </span>
                </td>

                <td>
                  <button
                    class="btn blue"
                    onclick="openAllocation('${esc(student[1])}')"
                  >
                    Allocate
                  </button>
                </td>

              </tr>
            `;
          }

          const room =
            findRoom(allocation.roomNumber);

          const occupied =
            getOccupiedBeds(
              allocation.roomNumber
            );

          const total =
            room ? room[2] : 0;

          const available =
            getAvailableBeds(
              allocation.roomNumber
            );

          const status =
            getRoomStatus(
              allocation.roomNumber
            );

          return `
            <tr>

              <td>
                ${cell(student[0], student[1])}
              </td>

              <td>
                <strong>
                  ${esc(allocation.roomNumber)}
                </strong>
              </td>

              <td>
                Bed ${esc(allocation.bedNumber)}
              </td>

              <td>
                ${occupied}/${total}
              </td>

              <td>

                ${available} available

                <br>

                <span class="status ${sc(status)}">
                  ${esc(status)}
                </span>

              </td>

              <td>

                <button
                  class="btn light"
                  onclick="changeAllocation('${esc(student[1])}')"
                >
                  Change
                </button>

                <button
                  class="btn danger"
                  onclick="removeAllocation('${esc(student[1])}')"
                >
                  Remove
                </button>

              </td>

            </tr>
          `;
        })
      )}
    `;
  },


  /* =======================================================
     PAYMENTS
     ======================================================= */

  payments() {
    return `
      ${head(
        'Payments',
        'Monitor student residence payments'
      )}

      ${table(
        [
          'Student',
          'Student Number',
          'Room',
          'Amount',
          'Status'
        ],

        students.map(student => {

          let payment =
            getStudentPayment(student[1]);

          if (!payment) {
            createPaymentForStudent(student);
            payment =
              getStudentPayment(student[1]);
          }

          const allocation =
            findStudentAllocation(student[1]);

          const room =
            allocation
              ? allocation.roomNumber +
                ' - Bed ' +
                allocation.bedNumber
              : 'Unallocated';

          return `
            <tr>

              <td>
                ${cell(student[0], student[1])}
              </td>

              <td>
                ${esc(student[1])}
              </td>

              <td>
                ${esc(room)}
              </td>

              <td>
                ${esc(payment.amount)}
              </td>

              <td>
                <span class="status ${sc(payment.status)}">
                  ${esc(payment.status)}
                </span>
              </td>

            </tr>
          `;
        })
      )}
    `;
  },


  /* =======================================================
     MAINTENANCE
     ======================================================= */

  maintenance() {
    return `
      ${head(
        'Maintenance',
        'Track residence maintenance requests',
        `
          <button
            class="btn blue"
            onclick="openMaintenance()"
          >
            + Add Request
          </button>
        `
      )}

      ${table(
        [
          'Issue',
          'Room',
          'Category',
          'Status',
          'Action'
        ],

        maintenance.map((item, index) => {

          const issue = item[0];
          const roomNumber = item[1];
          const category = item[2];
          const status = item[3];

          const affectedStudents =
            getMaintenanceAffectedStudents(
              roomNumber
            );

          return `
            <tr>

              <td>
                <strong>
                  ${esc(issue)}
                </strong>

                ${
                  affectedStudents.length > 0
                    ? `
                      <small>
                        ${affectedStudents.length}
                        student${
                          affectedStudents.length === 1
                            ? ''
                            : 's'
                        } affected
                      </small>
                    `
                    : ''
                }
              </td>

              <td>
                ${esc(roomNumber)}
              </td>

              <td>
                ${esc(category)}
              </td>

              <td>
                <span class="status ${sc(status)}">
                  ${esc(status)}
                </span>
              </td>

              <td>

                <button
                  class="btn light"
                  onclick="viewMaintenance(${index})"
                >
                  View
                </button>

                <button
                  class="btn blue"
                  onclick="updateMaintenance(${index})"
                >
                  Update
                </button>

              </td>

            </tr>
          `;
        })
      )}
    `;
  },


  /* =======================================================
     MESSAGES
     ======================================================= */

  messages() {
    return `
      ${head(
        'Messages',
        'Student communication and enquiries'
      )}

      ${table(
        [
          'Student',
          'Student Number',
          'Subject',
          'Message',
          'Date'
        ],

        students.flatMap(student => {

          let studentMessages =
            getStudentMessages(student[1]);

          if (studentMessages.length === 0) {
            createWelcomeMessage(student);

            studentMessages =
              getStudentMessages(student[1]);
          }

          return studentMessages.map(message => `
            <tr>

              <td>
                ${cell(student[0], student[1])}
              </td>

              <td>
                ${esc(student[1])}
              </td>

              <td>
                ${esc(message.subject)}
              </td>

              <td>
                ${esc(message.message)}
              </td>

              <td>
                ${esc(message.date)}
              </td>

            </tr>
          `);
        })
      )}
    `;
  },


  /* =======================================================
     REPORTS
     ======================================================= */

  reports() {
    const totalBeds =
      rooms.reduce(
        (total, room) => total + room[2],
        0
      );

    const occupiedBeds =
      allocations.length;

    const availableBeds =
      Math.max(
        0,
        totalBeds - occupiedBeds
      );

    return `
      ${head(
        'Reports',
        'Residence management reports and statistics'
      )}

      <div class="stats">

        <div class="stat">
          <div class="staticon">👨‍🎓</div>

          <div>
            <strong>${students.length}</strong>
            <span>Students</span>
          </div>
        </div>

        <div class="stat">
          <div class="staticon">🏠</div>

          <div>
            <strong>${rooms.length}</strong>
            <span>Rooms</span>
          </div>
        </div>

        <div class="stat">
          <div class="staticon">🛏</div>

          <div>
            <strong>${occupiedBeds}</strong>
            <span>Occupied Beds</span>
          </div>
        </div>

        <div class="stat">
          <div class="staticon">✓</div>

          <div>
            <strong>${availableBeds}</strong>
            <span>Available Beds</span>
          </div>
        </div>

      </div>

      <div class="card">

        <div class="cardhead">
          <div>
            <h3>Room Occupancy Report</h3>
            <p>
              Current occupancy for each residence room
            </p>
          </div>
        </div>

        ${table(
          [
            'Room',
            'Type',
            'Capacity',
            'Occupied',
            'Available',
            'Status'
          ],

          rooms.map(room => {

            const occupied =
              getOccupiedBeds(room[0]);

            const available =
              getAvailableBeds(room[0]);

            const status =
              getRoomStatus(room[0]);

            return `
              <tr>

                <td>
                  <strong>
                    ${esc(room[0])}
                  </strong>
                </td>

                <td>
                  ${esc(room[1])}
                </td>

                <td>
                  ${room[2]}
                </td>

                <td>
                  ${occupied}
                </td>

                <td>
                  ${available}
                </td>

                <td>
                  <span class="status ${sc(status)}">
                    ${esc(status)}
                  </span>
                </td>

              </tr>
            `;
          })
        )}

      </div>
    `;
  },


  /* =======================================================
     SETTINGS
     ======================================================= */

  settings() {
    return `
      ${head(
        'Settings',
        'Manage manager portal settings'
      )}

      <div class="card">

        <div class="cardhead">
          <div>
            <h3>Residence Settings</h3>
            <p>
              Configure your manager portal
            </p>
          </div>
        </div>

        <div class="formgrid">

          <div class="field">
            <label>Residence Name</label>
            <input value="Luhambo Residence">
          </div>

          <div class="field">
            <label>Manager Name</label>
            <input value="Residence Manager">
          </div>

          <div class="field">
            <label>Email</label>
            <input value="manager@luhambo.ac.za">
          </div>

          <div class="field">
            <label>Contact Number</label>
            <input value="+27 12 345 6789">
          </div>

        </div>

        <div class="modalactions">

          <button
            class="btn blue"
            onclick="toast('Settings saved successfully.')"
          >
            Save Changes
          </button>

        </div>

      </div>
    `;
  }
};


/* =========================================================
   APPLICATION FUNCTIONS
   ========================================================= */

function decision(id, status) {
  const application =
    apps.find(app => app.id === id);

  if (!application) {
    return;
  }

  application.status = status;

  if (status === 'Approved') {

    let student =
      findStudentByNumber(application.no);

    if (!student) {

      students.push([
        application.n,
        application.no,
        application.course,
        'Unallocated'
      ]);

      student =
        findStudentByNumber(application.no);
    }

    createPaymentForStudent(student);
    createWelcomeMessage(student);

    toast(
      application.n +
      ' has been registered at Luhambo Residence.'
    );
  }

  if (status === 'Rejected') {
    toast(
      application.n +
      '\'s application has been rejected.'
    );
  }

  updateBadge();

  page('applications');
}

function viewApplication(id) {
  const application =
    apps.find(app => app.id === id);

  if (!application) {
    return;
  }

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Application Details</h3>
            <p>
              Student accommodation application
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="details">

          <p>
            <strong>Name:</strong>
            ${esc(application.n)}
          </p>

          <p>
            <strong>Student Number:</strong>
            ${esc(application.no)}
          </p>

          <p>
            <strong>Course:</strong>
            ${esc(application.course)}
          </p>

          <p>
            <strong>Date:</strong>
            ${esc(application.date)}
          </p>

          <p>
            <strong>Status:</strong>
            ${esc(application.status)}
          </p>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Close
          </button>

        </div>

      </div>

    </div>
  `;
}


/* =========================================================
   STUDENT FUNCTIONS
   ========================================================= */

function openStudent(studentNumber = '') {
  const student =
    studentNumber
      ? findStudent(studentNumber)
      : null;

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>
              ${student
                ? 'Student Details'
                : 'Add Student'}
            </h3>

            <p>
              Student registration
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        ${
          student
            ? `
              <div class="details">

                <p>
                  <strong>Name:</strong>
                  ${esc(student[0])}
                </p>

                <p>
                  <strong>Student Number:</strong>
                  ${esc(student[1])}
                </p>

                <p>
                  <strong>Course:</strong>
                  ${esc(student[2])}
                </p>

                <p>
                  <strong>Room:</strong>
                  ${esc(student[3])}
                </p>

              </div>
            `
            : `
              <div class="formgrid">

                <div class="field">
                  <label>Full Name</label>

                  <input
                    id="studentName"
                    placeholder="Enter full name"
                  >
                </div>

                <div class="field">
                  <label>Student Number</label>

                  <input
                    id="studentNumber"
                    placeholder="Enter student number"
                  >
                </div>

                <div class="field">
                  <label>Course</label>

                  <input
                    id="studentCourse"
                    placeholder="Enter course"
                  >
                </div>

              </div>
            `
        }

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Close
          </button>

          ${
            !student
              ? `
                <button
                  class="btn blue"
                  onclick="saveStudent()"
                >
                  Save Student
                </button>
              `
              : ''
          }

        </div>

      </div>

    </div>
  `;
}

function saveStudent() {
  const name =
    document
      .getElementById('studentName')
      .value
      .trim();

  const number =
    document
      .getElementById('studentNumber')
      .value
      .trim();

  const course =
    document
      .getElementById('studentCourse')
      .value
      .trim();

  if (!name || !number || !course) {
    toast(
      'Please complete all student details.'
    );

    return;
  }

  const exists =
    students.some(
      student =>
        student[1].toLowerCase() ===
        number.toLowerCase()
    );

  if (exists) {
    toast(
      'A student with that student number already exists.'
    );

    return;
  }

  students.push([
    name,
    number,
    course,
    'Unallocated'
  ]);

  const student =
    findStudentByNumber(number);

  createPaymentForStudent(student);
  createWelcomeMessage(student);

  closeModal();

  toast(
    name +
    ' has been added successfully.'
  );

  page('students');
}


/* =========================================================
   ROOM FUNCTIONS
   ========================================================= */

function openRoom() {
  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Add Room</h3>
            <p>
              Create a new residence room
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="formgrid">

          <div class="field">
            <label>Room Number</label>

            <input
              id="roomNumber"
              placeholder="e.g. A105"
            >
          </div>

          <div class="field">
            <label>Room Type</label>

            <select id="roomType">

              <option value="Single">
                Single
              </option>

              <option value="Shared">
                Shared
              </option>

            </select>
          </div>

          <div class="field">
            <label>Total Beds</label>

            <input
              id="totalBeds"
              type="number"
              min="1"
              placeholder="e.g. 2"
            >
          </div>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Cancel
          </button>

          <button
            class="btn blue"
            onclick="addRoom()"
          >
            Add Room
          </button>

        </div>

      </div>

    </div>
  `;
}

function addRoom() {
  const roomNumber =
    document
      .getElementById('roomNumber')
      .value
      .trim()
      .toUpperCase();

  const roomType =
    document
      .getElementById('roomType')
      .value;

  const totalBeds =
    Number(
      document
        .getElementById('totalBeds')
        .value
    );

  if (!roomNumber || !roomType || !totalBeds) {
    toast(
      'Please complete all room details.'
    );

    return;
  }

  if (totalBeds < 1) {
    toast(
      'A room must have at least one bed.'
    );

    return;
  }

  const exists =
    rooms.some(
      room =>
        room[0].toLowerCase() ===
        roomNumber.toLowerCase()
    );

  if (exists) {
    toast(
      'That room already exists.'
    );

    return;
  }

  rooms.push([
    roomNumber,
    roomType,
    totalBeds,
    0
  ]);

  closeModal();

  toast(
    'Room ' +
    roomNumber +
    ' has been added.'
  );

  page('rooms');
}

function editRoom(roomNumber) {
  const room =
    findRoom(roomNumber);

  if (!room) {
    return;
  }

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Edit Room</h3>
            <p>
              Update room details
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="formgrid">

          <div class="field">
            <label>Room Number</label>

            <input
              id="editRoomNumber"
              value="${esc(room[0])}"
            >
          </div>

          <div class="field">
            <label>Room Type</label>

            <select id="editRoomType">

              <option
                value="Single"
                ${room[1] === 'Single' ? 'selected' : ''}
              >
                Single
              </option>

              <option
                value="Shared"
                ${room[1] === 'Shared' ? 'selected' : ''}
              >
                Shared
              </option>

            </select>
          </div>

          <div class="field">
            <label>Total Beds</label>

            <input
              id="editTotalBeds"
              type="number"
              min="1"
              value="${room[2]}"
            >
          </div>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Cancel
          </button>

          <button
            class="btn blue"
            onclick="saveRoomEdit('${esc(roomNumber)}')"
          >
            Save Changes
          </button>

        </div>

      </div>

    </div>
  `;
}

function saveRoomEdit(oldRoomNumber) {
  const room =
    findRoom(oldRoomNumber);

  if (!room) {
    return;
  }

  const newRoomNumber =
    document
      .getElementById('editRoomNumber')
      .value
      .trim()
      .toUpperCase();

  const newRoomType =
    document
      .getElementById('editRoomType')
      .value;

  const newTotalBeds =
    Number(
      document
        .getElementById('editTotalBeds')
        .value
    );

  const occupied =
    getOccupiedBeds(oldRoomNumber);

  if (
    !newRoomNumber ||
    !newRoomType ||
    !newTotalBeds
  ) {
    toast(
      'Please complete all room details.'
    );

    return;
  }

  if (newTotalBeds < occupied) {
    toast(
      'Total beds cannot be less than the number of occupied beds.'
    );

    return;
  }

  const duplicate =
    rooms.some(
      existingRoom =>
        existingRoom !== room &&
        existingRoom[0].toLowerCase() ===
        newRoomNumber.toLowerCase()
    );

  if (duplicate) {
    toast(
      'That room number already exists.'
    );

    return;
  }

  room[0] = newRoomNumber;
  room[1] = newRoomType;
  room[2] = newTotalBeds;

  allocations.forEach(allocation => {
    if (
      allocation.roomNumber ===
      oldRoomNumber
    ) {
      allocation.roomNumber =
        newRoomNumber;
    }
  });

  /*
    Maintenance records are also kept connected
    when a room number changes.
  */
  maintenance.forEach(item => {
    if (item[1] === oldRoomNumber) {
      item[1] = newRoomNumber;
    }
  });

  syncStudentRooms();

  closeModal();

  toast(
    'Room details updated successfully.'
  );

  page('rooms');
}


/* =========================================================
   ALLOCATION FUNCTIONS
   ========================================================= */

function openAllocation(studentNumber = '') {
  syncStudentRooms();

  const approvedStudents =
    students.filter(student => {

      const application =
        apps.find(
          app => app.no === student[1]
        );

      return application
        ? application.status === 'Approved'
        : true;
    });

  const availableRooms =
    rooms.filter(
      room =>
        getAvailableBeds(room[0]) > 0
    );

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Allocate Student</h3>

            <p>
              Assign a student to an available room and bed
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="formgrid">

          <div class="field">
            <label>Student</label>

            <select id="allocationStudent">

              <option value="">
                Select student
              </option>

              ${
                approvedStudents
                  .filter(
                    student =>
                      !findStudentAllocation(
                        student[1]
                      )
                  )
                  .map(student => `
                    <option
                      value="${esc(student[1])}"
                      ${
                        student[1] === studentNumber
                          ? 'selected'
                          : ''
                      }
                    >
                      ${esc(student[0])}
                      -
                      ${esc(student[1])}
                    </option>
                  `)
                  .join('')
              }

            </select>
          </div>

          <div class="field">
            <label>Room</label>

            <select id="allocationRoom">

              <option value="">
                Select room
              </option>

              ${
                availableRooms
                  .map(room => `
                    <option
                      value="${esc(room[0])}"
                    >
                      ${esc(room[0])}
                      -
                      ${esc(room[1])}
                      (
                      ${getAvailableBeds(room[0])}
                      available
                      )
                    </option>
                  `)
                  .join('')
              }

            </select>
          </div>

        </div>

        ${
          approvedStudents.filter(
            student =>
              !findStudentAllocation(
                student[1]
              )
          ).length === 0
            ? `
              <p class="empty">
                There are currently no unallocated approved students.
              </p>
            `
            : ''
        }

        ${
          availableRooms.length === 0
            ? `
              <p class="empty">
                There are currently no rooms with available beds.
              </p>
            `
            : ''
        }

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Cancel
          </button>

          <button
            class="btn blue"
            onclick="saveAllocation()"
            ${
              approvedStudents.filter(
                student =>
                  !findStudentAllocation(
                    student[1]
                  )
              ).length === 0 ||
              availableRooms.length === 0
                ? 'disabled'
                : ''
            }
          >
            Allocate
          </button>

        </div>

      </div>

    </div>
  `;
}

function saveAllocation() {
  const studentNumber =
    document
      .getElementById('allocationStudent')
      .value;

  const roomNumber =
    document
      .getElementById('allocationRoom')
      .value;

  if (!studentNumber || !roomNumber) {
    toast(
      'Please select a student and a room.'
    );

    return;
  }

  const student =
    findStudent(studentNumber);

  const room =
    findRoom(roomNumber);

  if (!student || !room) {
    toast(
      'The selected student or room could not be found.'
    );

    return;
  }

  if (
    findStudentAllocation(studentNumber)
  ) {
    toast(
      'This student is already allocated.'
    );

    return;
  }

  const availableBeds =
    getAvailableBeds(roomNumber);

  if (availableBeds <= 0) {
    toast(
      'This room is already full.'
    );

    return;
  }

  const bedNumber =
    getNextAvailableBed(roomNumber);

  if (!bedNumber) {
    toast(
      'No available bed could be found in this room.'
    );

    return;
  }

  allocations.push({
    studentNumber,
    roomNumber,
    bedNumber
  });

  syncStudentRooms();

  createPaymentForStudent(student);
  createWelcomeMessage(student);

  closeModal();

  toast(
    student[0] +
    ' has been allocated to ' +
    roomNumber +
    ', Bed ' +
    bedNumber +
    '.'
  );

  page('allocations');
}

function changeAllocation(studentNumber) {
  const allocation =
    findStudentAllocation(studentNumber);

  const student =
    findStudent(studentNumber);

  if (!allocation || !student) {
    toast(
      'This student does not have an allocation.'
    );

    return;
  }

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Change Allocation</h3>

            <p>
              Move the student to another available room
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="details">

          <p>
            <strong>Student:</strong>
            ${esc(student[0])}
          </p>

          <p>
            <strong>Current Room:</strong>
            ${esc(allocation.roomNumber)}
            -
            Bed ${esc(allocation.bedNumber)}
          </p>

        </div>

        <div class="formgrid">

          <div class="field">

            <label>New Room</label>

            <select id="changeAllocationRoom">

              ${
                rooms
                  .filter(room => {

                    if (
                      room[0] ===
                      allocation.roomNumber
                    ) {
                      return true;
                    }

                    return (
                      getAvailableBeds(
                        room[0]
                      ) > 0
                    );
                  })
                  .map(room => `
                    <option
                      value="${esc(room[0])}"
                      ${
                        room[0] ===
                        allocation.roomNumber
                          ? 'selected'
                          : ''
                      }
                    >
                      ${esc(room[0])}
                      -
                      ${esc(room[1])}
                      (
                      ${getAvailableBeds(room[0])}
                      available
                      )
                    </option>
                  `)
                  .join('')
              }

            </select>

          </div>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Cancel
          </button>

          <button
            class="btn blue"
            onclick="saveAllocationChange('${esc(studentNumber)}')"
          >
            Save Allocation
          </button>

        </div>

      </div>

    </div>
  `;
}

function saveAllocationChange(studentNumber) {
  const allocation =
    findStudentAllocation(studentNumber);

  if (!allocation) {
    toast(
      'Allocation not found.'
    );

    return;
  }

  const newRoomNumber =
    document
      .getElementById('changeAllocationRoom')
      .value;

  const oldRoomNumber =
    allocation.roomNumber;

  if (!newRoomNumber) {
    toast(
      'Please select a room.'
    );

    return;
  }

  if (
    newRoomNumber === oldRoomNumber
  ) {
    closeModal();

    toast(
      'The student is already in this room.'
    );

    return;
  }

  const newRoom =
    findRoom(newRoomNumber);

  if (!newRoom) {
    toast(
      'The selected room could not be found.'
    );

    return;
  }

  if (
    getAvailableBeds(newRoomNumber) <= 0
  ) {
    toast(
      'The selected room is full.'
    );

    return;
  }

  const newBedNumber =
    getNextAvailableBed(
      newRoomNumber
    );

  if (!newBedNumber) {
    toast(
      'No available bed could be found in this room.'
    );

    return;
  }

  allocation.roomNumber =
    newRoomNumber;

  allocation.bedNumber =
    newBedNumber;

  syncStudentRooms();

  const student =
    findStudent(studentNumber);

  createPaymentForStudent(student);
  createWelcomeMessage(student);

  closeModal();

  toast(
    student[0] +
    ' has been moved to ' +
    newRoomNumber +
    ', Bed ' +
    newBedNumber +
    '.'
  );

  page('allocations');
}

function removeAllocation(studentNumber) {
  const allocationIndex =
    allocations.findIndex(
      allocation =>
        allocation.studentNumber ===
        studentNumber
    );

  if (allocationIndex === -1) {
    toast(
      'This student does not have an allocation.'
    );

    return;
  }

  const student =
    findStudent(studentNumber);

  if (!student) {
    return;
  }

  allocations.splice(
    allocationIndex,
    1
  );

  syncStudentRooms();

  toast(
    student[0] +
    ' has been removed from their room allocation.'
  );

  page('allocations');
}


/* =========================================================
   MAINTENANCE FUNCTIONS
   ========================================================= */

function openMaintenance() {
  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Add Maintenance Request</h3>

            <p>
              Record a residence maintenance issue
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="formgrid">

          <div class="field">

            <label>Issue</label>

            <input
              id="maintenanceIssue"
              placeholder="e.g. Broken window"
            >

          </div>

          <div class="field">

            <label>Room</label>

            <select id="maintenanceRoom">

              <option value="">
                Select room
              </option>

              ${
                rooms.map(room => `
                  <option
                    value="${esc(room[0])}"
                  >
                    ${esc(room[0])}
                    -
                    ${esc(room[1])}
                  </option>
                `).join('')
              }

            </select>

          </div>

          <div class="field">

            <label>Category</label>

            <select id="maintenanceCategory">

              <option value="General">
                General
              </option>

              <option value="Plumbing">
                Plumbing
              </option>

              <option value="Electrical">
                Electrical
              </option>

              <option value="Furniture">
                Furniture
              </option>

              <option value="Cleaning">
                Cleaning
              </option>

              <option value="Security">
                Security
              </option>

            </select>

          </div>

          <div class="field">

            <label>Status</label>

            <select id="maintenanceStatus">

              <option value="Open">
                Open
              </option>

              <option value="In Progress">
                In Progress
              </option>

              <option value="Resolved">
                Resolved
              </option>

            </select>

          </div>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Cancel
          </button>

          <button
            class="btn blue"
            onclick="saveMaintenance()"
          >
            Save Request
          </button>

        </div>

      </div>

    </div>
  `;
}

function saveMaintenance() {
  const issue =
    document
      .getElementById('maintenanceIssue')
      .value
      .trim();

  const roomNumber =
    document
      .getElementById('maintenanceRoom')
      .value;

  const category =
    document
      .getElementById('maintenanceCategory')
      .value;

  const status =
    document
      .getElementById('maintenanceStatus')
      .value;

  if (
    !issue ||
    !roomNumber ||
    !category ||
    !status
  ) {
    toast(
      'Please complete all maintenance details.'
    );

    return;
  }

  const room =
    findRoom(roomNumber);

  if (!room) {
    toast(
      'The selected room could not be found.'
    );

    return;
  }

  maintenance.push([
    issue,
    roomNumber,
    category,
    status
  ]);

  const newMaintenance =
    maintenance[
      maintenance.length - 1
    ];

  createMaintenanceMessage(
    newMaintenance,
    status
  );

  closeModal();

  toast(
    'Maintenance request added successfully.'
  );

  page('maintenance');
}

function viewMaintenance(index) {
  const item =
    maintenance[index];

  if (!item) {
    return;
  }

  const issue =
    item[0];

  const roomNumber =
    item[1];

  const category =
    item[2];

  const status =
    item[3];

  const affectedStudents =
    getMaintenanceAffectedStudents(
      roomNumber
    );

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Maintenance Details</h3>

            <p>
              Residence maintenance request
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="details">

          <p>
            <strong>Issue:</strong>
            ${esc(issue)}
          </p>

          <p>
            <strong>Room:</strong>
            ${esc(roomNumber)}
          </p>

          <p>
            <strong>Category:</strong>
            ${esc(category)}
          </p>

          <p>
            <strong>Status:</strong>

            <span class="status ${sc(status)}">
              ${esc(status)}
            </span>
          </p>

          <p>
            <strong>Affected Students:</strong>

            ${
              affectedStudents.length > 0
                ? affectedStudents
                    .map(student =>
                      esc(student[0])
                    )
                    .join(', ')
                : 'No student currently allocated'
            }
          </p>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Close
          </button>

        </div>

      </div>

    </div>
  `;
}

function updateMaintenance(index) {
  const item =
    maintenance[index];

  if (!item) {
    return;
  }

  document.getElementById('modal').innerHTML = `
    <div class="modalback">

      <div class="modal">

        <div class="modalhead">

          <div>
            <h3>Update Maintenance</h3>

            <p>
              Update the maintenance request status
            </p>
          </div>

          <button
            class="close"
            onclick="closeModal()"
          >
            ×
          </button>

        </div>

        <div class="details">

          <p>
            <strong>Issue:</strong>
            ${esc(item[0])}
          </p>

          <p>
            <strong>Room:</strong>
            ${esc(item[1])}
          </p>

          <p>
            <strong>Category:</strong>
            ${esc(item[2])}
          </p>

        </div>

        <div class="formgrid">

          <div class="field">

            <label>Status</label>

            <select id="maintenanceUpdateStatus">

              <option
                value="Open"
                ${
                  item[3] === 'Open'
                    ? 'selected'
                    : ''
                }
              >
                Open
              </option>

              <option
                value="In Progress"
                ${
                  item[3] === 'In Progress'
                    ? 'selected'
                    : ''
                }
              >
                In Progress
              </option>

              <option
                value="Resolved"
                ${
                  item[3] === 'Resolved'
                    ? 'selected'
                    : ''
                }
              >
                Resolved
              </option>

            </select>

          </div>

        </div>

        <div class="modalactions">

          <button
            class="btn light"
            onclick="closeModal()"
          >
            Cancel
          </button>

          <button
            class="btn blue"
            onclick="saveMaintenanceStatus(${index})"
          >
            Save Changes
          </button>

        </div>

      </div>

    </div>
  `;
}

function saveMaintenanceStatus(index) {
  const item =
    maintenance[index];

  if (!item) {
    return;
  }

  const statusElement =
    document.getElementById(
      'maintenanceUpdateStatus'
    );

  if (!statusElement) {
    return;
  }

  const newStatus =
    statusElement.value;

  const oldStatus =
    item[3];

  if (!newStatus) {
    toast(
      'Please select a maintenance status.'
    );

    return;
  }

  item[3] = newStatus;

  if (oldStatus !== newStatus) {
    createMaintenanceMessage(
      item,
      newStatus
    );
  }

  closeModal();

  toast(
    'Maintenance request updated to ' +
    newStatus +
    '.'
  );

  page('maintenance');
}


/* =========================================================
   MODAL
   ========================================================= */

function closeModal() {
  const modal =
    document.getElementById('modal');

  if (modal) {
    modal.innerHTML = '';
  }
}


/* =========================================================
   NAVIGATION
   ========================================================= */

function updateBadge() {
  const badge =
    document.getElementById('badge');

  if (!badge) {
    return;
  }

  const pending =
    apps.filter(
      app => app.status === 'Pending'
    ).length;

  badge.textContent = pending;

  if (pending === 0) {
    badge.style.display = 'none';
  } else {
    badge.style.display = '';
  }
}

function loadPage(pageName) {
  if (!pages[pageName]) {
    pageName = 'dashboard';
  }

  syncStudentRooms();

  page(
    pages[pageName]()
  );

  document
    .querySelectorAll('[data-page]')
    .forEach(link => {

      link.classList.toggle(
        'active',
        link.dataset.page === pageName
      );

    });

  const title =
    document.getElementById('title');

  if (title) {

    const titles = {
      dashboard: 'Dashboard',
      applications: 'Applications',
      students: 'Students',
      rooms: 'Rooms & Beds',
      allocations: 'Room Allocation',
      payments: 'Payments',
      maintenance: 'Maintenance',
      messages: 'Messages',
      reports: 'Reports',
      settings: 'Settings'
    };

    title.textContent =
      titles[pageName] ||
      'Dashboard';
  }

  updateBadge();
}

document
  .querySelectorAll('[data-page]')
  .forEach(link => {

    link.addEventListener(
      'click',
      event => {

        event.preventDefault();

        const pageName =
          link.dataset.page;

        loadPage(pageName);

      }
    );

  });


/* =========================================================
   MOBILE MENU
   ========================================================= */

const menuButton =
  document.getElementById('menu');

const sidebar =
  document.getElementById('side');

if (menuButton && sidebar) {

  menuButton.addEventListener(
    'click',
    () => {
      sidebar.classList.toggle('open');
    }
  );

}


/* =========================================================
   LOGOUT
   ========================================================= */

const logoutButton =
  document.getElementById('logout');

if (logoutButton) {

  logoutButton.addEventListener(
    'click',
    () => {

      localStorage.removeItem(
        'safUser'
      );

      window.location.href =
        'login.html';

    }
  );

}


/* =========================================================
   INITIAL LOAD
   ========================================================= */

syncStudentRooms();

updateBadge();

loadPage('dashboard');