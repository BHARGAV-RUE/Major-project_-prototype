import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

export const startTransferSession = async (transferData) => {
  const response = await api.post('/transfers/start', transferData);
  return response.data;
};

export default api;