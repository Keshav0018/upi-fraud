const STORAGE_KEY = 'payguard.upiapp.thisPhoneDeviceId';

function randomDeviceId() {
  const random = (crypto.randomUUID && crypto.randomUUID()) || Math.random().toString(36).slice(2);
  return `device-${random}`;
}

export function getThisPhoneDeviceId() {
  try {
    let id = localStorage.getItem(STORAGE_KEY);
    if (!id) {
      id = randomDeviceId();
      localStorage.setItem(STORAGE_KEY, id);
    }
    return id;
  } catch {
    return randomDeviceId();
  }
}

export function newPhoneDeviceId() {
  return randomDeviceId();
}
