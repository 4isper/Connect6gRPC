module com.example.connect6grpc {
    requires javafx.controls;
    requires javafx.fxml;
    requires io.grpc.stub;
    requires io.grpc;
    requires annotations.api;
    requires com.google.protobuf;
    requires com.google.common;
    requires io.grpc.protobuf;

    opens com.example.connect6grpc to javafx.fxml;
    exports com.example.connect6grpc;
}