const devApi = 'http://localhost:3000';
const prodApi = 'https://api.cpicpgx.org/v1';

exports.apiUrl = (uri) => {
  if (!uri) return null;
  return (process.env.API === 'dev' ? devApi : prodApi) + uri;
};
