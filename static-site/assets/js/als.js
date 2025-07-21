document.getElementById('al-dropdown').addEventListener("change", e => {

  const al = e.target.value;
  const url = `/prod/setal?al=${al}`;
  fetch(url)
  .then(r => {

    if (r.ok) {
      location.href = `./legend/legend_${al}.html`;
    } else {
      console.error(`Failed to set admin language to ${al}`);
    }
  });
});

