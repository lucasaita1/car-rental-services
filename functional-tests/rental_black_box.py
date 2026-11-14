"""Testes funcionais (caixa-preta) da funcionalidade "Alugar um carro".

Os casos foram derivados apenas da especificação (regras RN01 a RN10 em
functional-tests/README.md) e são executados contra a API real, por HTTP.

Uso:
    ADMIN_EMAIL=... ADMIN_PASSWORD=... python3 functional-tests/rental_black_box.py \
        --user-api http://localhost:8093 --car-api http://localhost:8094 \
        --mysql-container ft-mysql --mysql-db cardb_ft \
        --redis-container ft-redis --out resultados.json

Pré-requisitos: serviços user e car no ar, apontando para bancos descartáveis.
Os containers são usados só para preparar duas pré-condições que a API não
permite criar (carro legado sem diária e sessão expirada).
"""
import argparse
import datetime as dt
import json
import os
import random
import string
import subprocess
import sys
import urllib.error
import urllib.request
import uuid

HOJE = dt.date.today()


def dia(n):
    return (HOJE + dt.timedelta(days=n)).isoformat()


class Api:
    def __init__(self, user_api, car_api):
        self.user_api = user_api.rstrip('/')
        self.car_api = car_api.rstrip('/')

    def call(self, method, url, body=None, token=None, raw=None, content_type='application/json'):
        data = raw if raw is not None else (json.dumps(body).encode() if body is not None else None)
        headers = {}
        if data is not None:
            headers['Content-Type'] = content_type
        if token:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(url, data=data, method=method, headers=headers)
        try:
            with urllib.request.urlopen(request, timeout=20) as response:
                return response.status, parse(response.read(), response.headers.get('Content-Type', ''))
        except urllib.error.HTTPError as error:
            return error.code, parse(error.read(), error.headers.get('Content-Type', ''))

    def login(self, email, password):
        status, body = self.call('POST', f'{self.user_api}/auth/users/login', {'email': email, 'password': password})
        assert status == 200, f'login falhou: {status} {body}'
        return body['token'], int(body['user']['id'])

    def register(self, prefix, with_cnh):
        email = f'{prefix}-{uuid.uuid4().hex[:8]}@ft.test'
        password = 'SenhaTeste123'
        status, body = self.call('POST', f'{self.user_api}/users/register', {
            'name': f'Cliente {prefix}', 'email': email, 'cpf': '12345678901',
            'cnh': '98765432100', 'password': password})
        assert status == 201, f'cadastro falhou: {status} {body}'
        token, user_id = self.login(email, password)
        if with_cnh:
            status, body = self.upload_cnh(token)
            assert status == 200, f'envio da CNH falhou: {status} {body}'
        return {'token': token, 'id': user_id, 'email': email}

    def upload_cnh(self, token):
        boundary = '----ft' + uuid.uuid4().hex
        pdf = b'%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n'
        body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="cnh.pdf"\r\n'
                'Content-Type: application/pdf\r\n\r\n').encode() + pdf + f'\r\n--{boundary}--\r\n'.encode()
        return self.call('PUT', f'{self.user_api}/users/me/cnh-document', raw=body, token=token,
                         content_type='multipart/form-data; boundary=' + boundary)

    def create_car(self, admin, daily_rate=150.00, status=None, details=None):
        plate = 'FT' + random.choice(string.ascii_uppercase) + str(random.randint(0, 9)) \
            + random.choice(string.ascii_uppercase) + f'{random.randint(0, 99):02d}'
        body = {'model': 'Carro de Teste', 'color': 'Branco', 'plate': plate, 'year': 2024, 'dailyRate': daily_rate}
        if status:
            body['status'] = status
        if details is not None:
            body['details'] = details
        return self.call('POST', f'{self.car_api}/cars', body, admin)

    def car(self, car_id):
        return self.call('GET', f'{self.car_api}/cars/{car_id}')[1]

    def hold(self, car_id, token):
        return self.call('POST', f'{self.car_api}/rental/hold/{car_id}', token=token)

    def release(self, car_id, token):
        return self.call('DELETE', f'{self.car_api}/rental/hold/{car_id}', token=token)

    def rent(self, car_id, user_id, token, date=None, raw_date=None):
        query = ''
        if raw_date is not None:
            query = '?expectedReturnDate=' + urllib.request.quote(raw_date)
        elif date is not None:
            query = '?expectedReturnDate=' + date
        return self.call('POST', f'{self.car_api}/rental/rent/{car_id}/user/{user_id}{query}', token=token)

    def return_car(self, car_id, token):
        return self.call('POST', f'{self.car_api}/rental/return/{car_id}', token=token)

    def rentals(self, user_id, token):
        return self.call('GET', f'{self.car_api}/rental/user/{user_id}', token=token)[1]


def parse(raw, content_type):
    if 'json' in content_type:
        try:
            return json.loads(raw)
        except ValueError:
            pass
    return raw.decode('utf-8', 'replace')


def message(body):
    if isinstance(body, dict):
        return body.get('message') or body.get('error') or json.dumps(body, ensure_ascii=False)
    return str(body)


class Suite:
    def __init__(self, api, admin, args):
        self.api = api
        self.admin = admin
        self.args = args
        self.results = []

    def sql(self, statement):
        subprocess.run(['docker', 'exec', self.args.mysql_container, 'mysql', '-uroot',
                        f'-p{self.args.mysql_root_password}', self.args.mysql_db, '-e', statement],
                       check=True, capture_output=True)

    def expire_session(self, user_id):
        subprocess.run(['docker', 'exec', self.args.redis_container, 'redis-cli', 'DEL', f'user:{user_id}'],
                       check=True, capture_output=True)

    def new_car(self, **kwargs):
        status, body = self.api.create_car(self.admin, **kwargs)
        assert status == 200, f'cadastro do carro falhou: {status} {body}'
        return body['id']

    def record(self, case_id, title, expected, actual, passed, http_status=None):
        self.results.append({'id': case_id, 'title': title, 'expected': expected, 'actual': actual,
                             'http': http_status, 'passed': bool(passed)})
        mark = 'OK   ' if passed else 'FALHA'
        print(f'{mark} {case_id} {title}\n       esperado: {expected}\n       obtido:   {actual}')

    def rent_case(self, case_id, title, days, rate, expected_total, cliente):
        car_id = self.new_car(daily_rate=rate)
        self.api.hold(car_id, cliente['token'])
        status, body = self.api.rent(car_id, cliente['id'], cliente['token'], date=dia(days))
        rental = next((r for r in self.api.rentals(cliente['id'], cliente['token']) if r['carId'] == car_id), None)
        car = self.api.car(car_id)
        got_total = rental['estimatedTotal'] if rental else None
        ok = (status == 200 and 'sucesso' in str(body) and rental is not None
              and abs(float(got_total) - expected_total) < 0.005 and car['status'] == 'RENTED')
        actual = (f'HTTP {status}, "{message(body)[:70]}", total previsto = {got_total}, carro = {car.get("status")}'
                  if rental else f'HTTP {status}, "{message(body)[:90]}"')
        self.record(case_id, title, f'sucesso, total previsto = {expected_total:.2f}, carro = RENTED',
                    actual, ok, status)

    def refuse_case(self, case_id, title, status, body, expected_status, expected_text, car_id=None, user=None):
        no_rental = True
        if car_id is not None and user is not None:
            no_rental = all(r['carId'] != car_id for r in self.api.rentals(user['id'], user['token']))
        ok = status == expected_status and expected_text.lower() in message(body).lower() and no_rental
        self.record(case_id, title, f'HTTP {expected_status}, "{expected_text}", nenhuma locação criada',
                    f'HTTP {status}, "{message(body)[:110]}"' + ('' if no_rental else ', LOCAÇÃO CRIADA'),
                    ok, status)

    def run(self):
        api = self.api
        cliente = api.register('cnh', with_cnh=True)
        outro = api.register('outro', with_cnh=True)
        sem_cnh = api.register('semcnh', with_cnh=False)
        admin_id = self.args.admin_id

        # ---------- Data de devolução: classes de equivalência e valor-limite ----------
        self.rent_case('CT01', 'Devolução hoje (limite inferior válido)', 0, 150.00, 150.00, cliente)
        self.rent_case('CT02', 'Devolução amanhã (hoje + 1)', 1, 150.00, 150.00, cliente)
        self.rent_case('CT03', 'Devolução em hoje + 2', 2, 150.00, 300.00, cliente)
        self.rent_case('CT04', 'Devolução em 30 dias (classe válida típica)', 30, 150.00, 4500.00, cliente)
        bissexto = (dt.date(2028, 3, 1) - HOJE).days
        self.rent_case('CT05', f'Devolução em 01/03/2028, atravessa 29/02 ({bissexto} dias)', bissexto, 150.00,
                       150.00 * bissexto, cliente)

        car_id = self.new_car()
        status, body = api.rent(car_id, cliente['id'], cliente['token'], date=dia(-1))
        self.refuse_case('CT06', 'Devolução ontem (hoje - 1, limite inválido)', status, body, 200,
                         'não pode ser anterior a hoje', car_id, cliente)
        status, body = api.rent(car_id, cliente['id'], cliente['token'])
        self.refuse_case('CT07', 'Sem data de devolução', status, body, 400,
                         'Informe a data prevista de devolução', car_id, cliente)
        status, body = api.rent(car_id, cliente['id'], cliente['token'], raw_date='15/12/2026')
        self.refuse_case('CT08', 'Data em formato inválido (15/12/2026)', status, body, 400, '', car_id, cliente)
        status, body = api.rent(car_id, cliente['id'], cliente['token'], raw_date='2027-02-29')
        self.refuse_case('CT09', 'Data inexistente (29/02/2027)', status, body, 400, '', car_id, cliente)

        # ---------- Diária: valor-limite no cadastro e efeito no total ----------
        for case_id, title, rate, expected in [
            ('CT10', 'Diária R$ 0,99 (abaixo do mínimo)', 0.99, 400),
            ('CT11', 'Diária R$ 1,00 (mínimo)', 1.00, 200),
            ('CT12', 'Diária R$ 1,01 (logo acima do mínimo)', 1.01, 200),
            ('CT13', 'Diária R$ 99.999.999,99 (máximo)', 99999999.99, 200),
            ('CT14', 'Diária R$ 100.000.000,00 (acima do máximo)', 100000000.00, 400),
            ('CT15', 'Diária R$ 1,001 (três casas decimais)', 1.001, 400),
        ]:
            status, body = api.create_car(self.admin, daily_rate=rate)
            self.record(case_id, title, f'HTTP {expected}' + (' (cadastro aceito)' if expected == 200 else ' (cadastro recusado)'),
                        f'HTTP {status}' + (f', diária gravada = {body.get("dailyRate")}' if status == 200 else f', "{message(body)[:80]}"'),
                        status == expected, status)

        self.rent_case('CT16', 'Diária mínima R$ 1,00 por 1 dia', 1, 1.00, 1.00, cliente)
        self.rent_case('CT17', 'Diária máxima por 100 dias (maior total que cabe)', 100, 99999999.99,
                       9999999999.00, cliente)
        car_id = self.new_car(daily_rate=99999999.99)
        api.hold(car_id, cliente['token'])
        status, body = api.rent(car_id, cliente['id'], cliente['token'], date=dia(101))
        rental = next((r for r in api.rentals(cliente['id'], cliente['token']) if r['carId'] == car_id), None)
        controlled = status < 500
        self.record('CT18', 'Diária máxima por 101 dias (total passa de R$ 9.999.999.999,99)',
                    'resposta controlada (aceitar com o total certo ou recusar com mensagem); nunca erro interno 5xx',
                    f'HTTP {status}, "{message(body)[:90]}"' + (f', total = {rental["estimatedTotal"]}' if rental else ''),
                    controlled, status)

        # ---------- Tabela de decisão: condições do carro, da reserva e do cliente ----------
        car_id = self.new_car(status='MAINTENANCE')
        status, body = api.hold(car_id, cliente['token'])
        self.refuse_case('CT19', 'Carro em manutenção: reservar no checkout', status, body, 409, 'manutenção')
        status, body = api.rent(car_id, cliente['id'], cliente['token'], date=dia(3))
        self.refuse_case('CT20', 'Carro em manutenção: confirmar aluguel', status, body, 200, 'manutenção', car_id, cliente)

        car_id = self.new_car()
        self.sql(f'UPDATE car_model SET daily_rate = NULL WHERE id = {car_id}')
        status, body = api.rent(car_id, cliente['id'], cliente['token'], date=dia(3))
        self.refuse_case('CT21', 'Carro sem diária definida (cadastro legado)', status, body, 200,
                         'não tem valor de diária', car_id, cliente)

        car_id = self.new_car()
        api.hold(car_id, outro['token'])
        api.rent(car_id, outro['id'], outro['token'], date=dia(3))
        status, body = api.rent(car_id, cliente['id'], cliente['token'], date=dia(3))
        self.refuse_case('CT22', 'Carro já alugado por outro cliente', status, body, 200, 'já está alugado', car_id, cliente)

        car_id = self.new_car()
        api.hold(car_id, outro['token'])
        status, body = api.rent(car_id, cliente['id'], cliente['token'], date=dia(3))
        self.refuse_case('CT23', 'Carro reservado no checkout por outro cliente', status, body, 200,
                         'reservado por outro cliente', car_id, cliente)
        api.release(car_id, outro['token'])

        car_id = self.new_car()
        status, body = api.hold(car_id, sem_cnh['token'])
        self.refuse_case('CT24', 'Cliente sem PDF da CNH: reservar', status, body, 409, 'CNH')
        status, body = api.rent(car_id, sem_cnh['id'], sem_cnh['token'], date=dia(3))
        self.refuse_case('CT25', 'Cliente sem PDF da CNH: confirmar aluguel', status, body, 200, 'CNH', car_id, sem_cnh)

        expirado = api.register('expirado', with_cnh=True)
        self.expire_session(expirado['id'])
        car_id = self.new_car()
        status, body = api.rent(car_id, expirado['id'], expirado['token'], date=dia(3))
        self.refuse_case('CT26', 'Sessão do cliente expirada no servidor', status, body, 200,
                         'login novamente', car_id, expirado)

        status, body = api.rent(999999, cliente['id'], cliente['token'], date=dia(3))
        self.refuse_case('CT27', 'Carro inexistente (id 999999)', status, body, 404, 'não encontrado')

        # ---------- Quem aluga: classes de equivalência de autorização ----------
        car_id = self.new_car()
        status, body = api.rent(car_id, cliente['id'], None, date=dia(3))
        self.refuse_case('CT28', 'Sem token de acesso', status, body, 401, '', car_id, cliente)
        status, body = api.rent(car_id, outro['id'], cliente['token'], date=dia(3))
        self.refuse_case('CT29', 'Cliente tenta alugar em nome de outro cliente', status, body, 403,
                         'sua própria conta', car_id, outro)
        status, body = api.rent(car_id, cliente['id'], self.admin, date=dia(3))
        rental = next((r for r in api.rentals(cliente['id'], cliente['token']) if r['carId'] == car_id), None)
        self.record('CT30', 'Administrador aluga em nome de cliente com CNH',
                    'sucesso, locação registrada no nome do cliente',
                    f'HTTP {status}, "{message(body)[:60]}"' + (', locação no nome do cliente' if rental else ''),
                    status == 200 and rental is not None, status)
        car_id = self.new_car()
        status, body = api.rent(car_id, sem_cnh['id'], self.admin, date=dia(3))
        self.refuse_case('CT31', 'Administrador aluga em nome de cliente sem CNH', status, body, 200, 'CNH', car_id, sem_cnh)

        # ---------- Transição de estados do carro ----------
        car_id = self.new_car()
        estados = [self.api.car(car_id)['status'] + (' (reservado)' if self.api.car(car_id)['reserved'] else '')]
        api.hold(car_id, cliente['token'])
        c = api.car(car_id); estados.append(c['status'] + (' (reservado)' if c['reserved'] else ''))
        api.rent(car_id, cliente['id'], cliente['token'], date=dia(2))
        c = api.car(car_id); estados.append(c['status'] + (' (reservado)' if c['reserved'] else ''))
        api.return_car(car_id, cliente['token'])
        c = api.car(car_id); estados.append(c['status'] + (' (reservado)' if c['reserved'] else ''))
        esperado = ['AVAILABLE', 'AVAILABLE (reservado)', 'RENTED', 'AVAILABLE']
        rental = next((r for r in api.rentals(cliente['id'], cliente['token']) if r['carId'] == car_id), {})
        self.record('CT32', 'Ciclo completo: disponível → reservado → alugado → devolvido',
                    ' → '.join(esperado) + ', locação FINISHED com total final = 150,00',
                    ' → '.join(estados) + f', locação {rental.get("status")} com total final = {rental.get("totalAmount")}',
                    estados == esperado and rental.get('status') == 'FINISHED' and rental.get('totalAmount') == 150.0)

        car_id = self.new_car()
        api.hold(car_id, cliente['token'])
        status, _ = api.release(car_id, cliente['token'])
        c = api.car(car_id)
        self.record('CT33', 'Reservado → cancelar reserva volta a disponível',
                    'HTTP 204, carro AVAILABLE sem reserva',
                    f'HTTP {status}, carro {c["status"]}' + (' com reserva' if c['reserved'] else ' sem reserva'),
                    status == 204 and c['status'] == 'AVAILABLE' and not c['reserved'], status)

        car_id = self.new_car()
        status, body = api.return_car(car_id, self.admin)
        self.refuse_case('CT34', 'Transição inválida: devolver carro que está disponível', status, body, 200,
                         'não há aluguel ativo')

        car_id = self.new_car()
        api.hold(car_id, outro['token'])
        api.rent(car_id, outro['id'], outro['token'], date=dia(2))
        status, body = api.hold(car_id, cliente['token'])
        self.refuse_case('CT35', 'Transição inválida: reservar carro que está alugado', status, body, 409, 'já está alugado')

        return self.results


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--user-api', default='http://localhost:8093')
    parser.add_argument('--car-api', default='http://localhost:8094')
    parser.add_argument('--mysql-container', default='ft-mysql')
    parser.add_argument('--mysql-root-password', default='ftroot')
    parser.add_argument('--mysql-db', default='cardb_ft')
    parser.add_argument('--redis-container', default='ft-redis')
    parser.add_argument('--out', default='resultados.json')
    args = parser.parse_args()

    api = Api(args.user_api, args.car_api)
    admin_token, admin_id = api.login(os.environ['ADMIN_EMAIL'], os.environ['ADMIN_PASSWORD'])
    args.admin_id = admin_id
    results = Suite(api, admin_token, args).run()

    passed = sum(r['passed'] for r in results)
    print(f'\n{passed} de {len(results)} casos passaram')
    with open(args.out, 'w', encoding='utf-8') as f:
        json.dump({'executedAt': dt.datetime.now().isoformat(timespec='seconds'), 'today': HOJE.isoformat(),
                   'results': results}, f, ensure_ascii=False, indent=2)
    sys.exit(0 if passed == len(results) else 1)


if __name__ == '__main__':
    main()
