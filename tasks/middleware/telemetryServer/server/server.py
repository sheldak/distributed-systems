from concurrent import futures

import grpc

import telemetry_pb2 as telemetry_pb2
import telemetry_pb2_grpc as telemetry_pb2_grpc


class Telemetry(telemetry_pb2_grpc.TelemetryServicer):
    def SaveTemperature(self, request, context):
        try:
            info = request.info
            temperature = request.temperature
            message = log_measurement(info, f"{temperature:.1f} degrees", "temperature")
            return telemetry_pb2.Reply(status=0, message=message)
        except Exception as ex:
            return telemetry_pb2.Reply(status=1, message=f"Server error: {ex}")

    def SaveHumidity(self, request, context):
        try:
            info = request.info
            humidity = request.humidity
            message = log_measurement(info, f"{humidity}%", "humidity")
            return telemetry_pb2.Reply(status=0, message=message)
        except Exception as ex:
            return telemetry_pb2.Reply(status=1, message=f"Server error: {ex}")

    def SaveWeather(self, request, context):
        try:
            info = request.info
            weather = list(map(lambda value: telemetry_pb2.Weather.WeatherType.Name(value), request.weather_type))
            message = log_measurement(info, f"characteristics of current weather: {weather}", "weather")
            return telemetry_pb2.Reply(status=0, message=message)
        except Exception as ex:
            return telemetry_pb2.Reply(status=1, message=f"Server error: {ex}")


def log_measurement(info, measurement, measurement_type):
    message = \
        f"--------------\n" + \
        f"Type: {measurement_type}\n" + \
        f"Subscriber: {info.subscriber}\n" + \
        f"Time: {info.timestamp}\n" + \
        f"Position: ({info.location_x}, {info.location_y})\n" + \
        f"Measurement: {measurement}\n" + \
        f"--------------"

    print(message)
    return message


def serve():
    server = grpc.server(futures.ThreadPoolExecutor(max_workers=10))
    telemetry_pb2_grpc.add_TelemetryServicer_to_server(Telemetry(), server)
    server.add_insecure_port('localhost:50051')
    server.start()
    server.wait_for_termination()


if __name__ == '__main__':
    serve()
