const listContainer = document.querySelector("#service-list");
const serviceUrlInput = document.querySelector("#service-url");
const serviceNameInput = document.querySelector("#service-name");
const saveButton = document.querySelector("#post-service");

let servicesRequest = new Request('/service');
fetch(servicesRequest)
.then(function(response) {
    console.log(response)
    return response.json(); })
.then(function(serviceList) {
    console.log(serviceList)
  // serviceList.forEach(service => {
  //   var li = document.createElement("li");
  //   li.appendChild(document.createTextNode(service.url + ': ' + service.status));
  //   listContainer.appendChild(li);
  // });
});

saveButton.onclick = (evt) => {
  if(!validURL(serviceUrlInput.value)) {
    serviceUrlInput.value = "";
    return;
  };
  const newService = document.createElement("li");
  
  const newServiceInfo = document.createElement("div");
  newServiceInfo.className = "service-info";
  
  const serviceUrl = document.createElement("span");
  serviceUrl.innerHTML = serviceUrlInput.value;
  serviceUrl.className = "item-service-url";

  const serviceName = document.createElement("span");
  serviceName.innerHTML = `${serviceNameInput.value}: `;
  serviceName.className = "item-service-name";

  newServiceInfo.appendChild(serviceName);
  newServiceInfo.appendChild(serviceUrl);

  newService.appendChild(newServiceInfo);
  listContainer.appendChild(newService);


  setTimeout(function () {
    newService.className = newService.className + " show";
  }, 10);
  //     fetch('/service', {
  //     method: 'post',
  //     headers: {
  //     'Accept': 'application/json, text/plain, */*',
  //     'Content-Type': 'application/json'
  //     },
  //   body: JSON.stringify({url:urlName})
  // }).then(res=> location.reload());
};

function validURL(str) {
  var pattern = new RegExp(
    "^(https?:\\/\\/){1}" + // protocol
    "((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|" + // domain name
    "((\\d{1,3}\\.){3}\\d{1,3}))" + // OR ip (v4) address
    "(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*" + // port and path
    "(\\?[;&a-z\\d%_.~+=-]*)?" + // query string
      "(\\#[-a-z\\d_]*)?$",
    "i"
  ); // fragment locator

  console.log("Validate", !!pattern.test(str));
  return !!pattern.test(str);
}
