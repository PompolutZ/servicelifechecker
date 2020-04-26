const listContainer = document.querySelector("#service-list");
const serviceUrlInput = document.querySelector("#service-url");
const serviceNameInput = document.querySelector("#service-name");
const saveButton = document.querySelector("#post-service");

let servicesRequest = new Request("/service");
fetch(servicesRequest)
  .then(function (response) {
    console.log(response);
    return response.json();
  })
  .then(function (serviceList) {
    console.log(serviceList);
    serviceList.forEach((service) => {
      console.log(service)
      const li = createServiceItem(service);
      li.className = "show";
      console.log(li);
      listContainer.appendChild(li);
    });
  });

saveButton.onclick = (evt) => {
  if (!validURL(serviceUrlInput.value)) {
    serviceUrlInput.value = "";
    return;
  }

  const params = {
    url: serviceUrlInput.value,
    name: serviceNameInput.value,
    status: -1,
  };

  const newService = createServiceItem(params);
  listContainer.appendChild(newService);

  setTimeout(function () {
    newService.className = newService.className + " show";

    fetch("/service", {
      method: "post",
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ url: serviceUrlInput.value, name: serviceNameInput.value }),
    }).then((res) => location.reload());
  }, 10);
};

function getStatusIcon(statusCode) {
  const icon = document.createElement("i");
  if (statusCode < 0) {
    icon.className = "far fa-question-circle status-icon";
  } else if (statusCode === 200) {
    icon.className = "far fa-grin-beam status-icon status-ok";
  } else {
    icon.className = "far fa-dizzy status-icon status-fail";
  }

  return icon;
}

function createServiceItem({ url, name, status }) {
  const newService = document.createElement("li");

  const newServiceInfo = document.createElement("div");
  newServiceInfo.className = "service-info";

  const serviceUrl = document.createElement("span");
  serviceUrl.innerHTML = url;
  serviceUrl.className = "item-service-url";

  const serviceName = document.createElement("span");
  serviceName.innerHTML = `${name ? name : "Service"}: `;
  serviceName.className = "item-service-name";

  newServiceInfo.appendChild(serviceName);
  newServiceInfo.appendChild(serviceUrl);

  const deleteIcon = document.createElement("i");
  deleteIcon.className = "far fa-trash-alt delete-icon"
  deleteIcon.onclick = event => deleteItem(url)(event);

  newService.appendChild(newServiceInfo);
  newService.appendChild(getStatusIcon(status));
  newService.appendChild(deleteIcon);

  return newService;
}

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

const deleteItem = url => e => {
  fetch("/service", {
    method: "delete",
    headers: {
      Accept: "application/json, text/plain, */*",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ url: url }),
  }).then((res) => location.reload());
}
