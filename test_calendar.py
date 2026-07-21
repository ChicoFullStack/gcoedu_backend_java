import requests

url = "http://localhost:8080/login/"
data = {
    "registration": "moises@gcoedu.com.br",
    "password": "12345678"
}
response = requests.post(url, json=data)
print("Login:", response.status_code, response.text)

token = None
if response.status_code == 200:
    token = response.json().get("access_token")

if token:
    headers = {"Authorization": f"Bearer {token}"}
    r = requests.get("http://localhost:8080/calendar/my-events", headers=headers)
    print("Calendar:", r.status_code, r.text)
else:
    print("No token")
