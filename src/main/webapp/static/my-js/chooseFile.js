$(function () {
    $("#chooseFileForm").submit(function (e) {
        alert("chooseFile!!");
        var filePath = $("#inputfile").val();
        alert(filePath);
        e.preventDefault();
    });

});
