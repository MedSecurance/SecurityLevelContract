function download(requirements) {
  console.log(requirements);

  var doc = document.implementation.createDocument("", "", null);
  var requirementElements = doc.createElement("requirements");

  for (let requirement of requirements) {
    let requirementElement = doc.createElement("requirement");
    requirementElement.set(requirement.id);
    requirementElements.appendChild(requirementElement);
  }

  const s = new XMLSerializer();
  const str = s.serializeToString(doc);

  /*var save = document.getElementById("sample").value;
  var blob = new Blob([save], {
    type: "text/plain;charset=utf-8"
  });*/
  var blob = new Blob([str], {type: "application/xml;charset=utf-8"});
  saveAs(blob, "hello world.xml");
}
