import React from 'react';

export default function AppDay2({ title, count, user, hobbies, greetUser }) {
  return (
    <div>
      <h1>{title}</h1>
      <p>Count: {count}</p>
      <h2>User Info:</h2>
      <p>Name: {user.name}</p>
      <p>Age: {user.age}</p>
      <h3>Hobbies:</h3>
      <ul>
        {hobbies.map((hobby, index) => (
          <li key={index}>{hobby}</li>
        ))}
      </ul>
      <button onClick={greetUser}>Greet User</button>
    </div>
  );
}
