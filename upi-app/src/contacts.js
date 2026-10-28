export const PAYER_VPA = 'keshav.chandak@oksbi';
export const PAYER_NAME = 'Keshav Chandak';
export const STARTING_BALANCE = 42350;

export const VPA_REGEX = /^[a-z0-9._-]{2,}@[a-z]{2,}$/;

export const CONTACTS = [
  { name: 'Priya Sharma', vpa: 'priya.sharma@ybl' },
  { name: 'Rohan Verma', vpa: 'rohan.verma@paytm' },
  { name: 'Ananya Iyer', vpa: 'ananya.iyer@okhdfcbank' },
  { name: 'Karan Mehta', vpa: 'karan.mehta@oksbi' },
  { name: 'Sneha Reddy', vpa: 'sneha.reddy@okaxis' },
  { name: 'Vikram Rao', vpa: 'vikram.rao@ybl' },
];

export function randomContact() {
  return CONTACTS[Math.floor(Math.random() * CONTACTS.length)];
}

export function randomNewPayeeVpa() {
  return `newmerchant${Math.floor(Math.random() * 100000)}@okaxis`;
}

export function formatRupees(amount) {
  return `₹${Number(amount).toLocaleString('en-IN')}`;
}
