import React from 'react';

export default function AppDay1() {
  return (
    <>
      {/* 
        The empty tags <> and </> are called React Fragments. 
        They allow you to group multiple elements without adding an extra node to the DOM.
      */}
      <div>
        <h1>Welcome to Day 1!</h1>
        <p>This is your first component in React.</p>
      </div>
      <div>
        <h2>Understanding Fragments</h2>
        <p>
          Using fragments, we can return multiple elements without wrapping them in an additional
          parent element.
        </p>
      </div>
    </>
  );
}
