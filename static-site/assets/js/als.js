document.getElementById('al-dropdown').addEventListener("change", e => {

  dialang.session.al = e.target.value;
  dialang.switchState("legend");
});

