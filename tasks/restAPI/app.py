# `flask run` in main directory

from flask import Flask, render_template, request
from forms import SearchForm
from requests_futures.sessions import FuturesSession
import json

app = Flask(__name__)
app.config['SECRET_KEY'] = '5791628bb0b13ce0c676dfde280ba245'
currencies = ["PLN", "USD", "EUR", "CHF", "GBP", "JPY", "CNY"]


@app.route("/", methods=['GET', 'POST'])
def search_func():
    form = SearchForm()

    try:
        base = request.form['base']
        data = get_data(base)
        return render_template('search.html', title='Search', form=form, data=data)
    except:
        return render_template('search.html', title='Search', form=form, data=[])


def get_data(base):
    session = FuturesSession()
    future_names = session.get("https://api.coinbase.com/v2/currencies")
    future_rates = session.get(f"https://api.exchangerate-api.com/v4/latest/{base}")

    names = json.loads(future_names.result().text)["data"]
    for currency in names:
        if currency["id"] == base:
            name = currency["name"]
            break
    else:
        name = ""
    rates = json.loads(future_rates.result().text)["rates"]
    data = [f"Base: {name} ({base})"] if name != "" else []
    for currency in currencies:
        data.append(f'{currency}: {rates[currency]}')

    return data


if __name__ == "__main__":
    app.run()
