import React from 'react';
import ReactDOM from 'react-dom';
import AppDay2 from './AppDay2';

const title = "Props Example with Function";
const count = 5;
const user = { name: "Alice", age: 30 };
const hobbies = ["Reading", "Traveling", "Cooking"];

const greetUser = () => {
  alert(`Hello, ${user.name}!`);
};

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <AppDay2 
      title={title} 
      count={count} 
      user={user} 
      hobbies={hobbies} 
      greetUser={greetUser} 
    />
  </React.StrictMode>,
  document.getElementById('root')
);
