document.getElementById('al-dropdown').addEventListener("change", e => {

  const al = e.target.value;

  dialang.session.al = al;
  console.log(`Session ID: ${sessionId}`);
  dialang.switchState("legend");

  /*
  const url = `/prod/setal?al=${al}`;
  fetch(url)
  .then(r => {

    if (r.ok) {
      return r.text();
    }

    throw new Error(`Failed to set admin language to ${al}`);
  })
  .then(sessionId => {

    dialang.session.id = sessionId;
    dialang.session.al = al;
    console.log(`Session ID: ${sessionId}`);
    dialang.switchState("legend");
  })
  .catch(err => {
    console.error(`Failed to set admin language to ${al}`, err);
  });
  */
});

