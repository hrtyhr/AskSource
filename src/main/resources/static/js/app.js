(function () {
    "use strict";

    document.addEventListener("DOMContentLoaded", function () {
        document.querySelectorAll(".flash").forEach(function (flash) {
            window.setTimeout(function () {
                flash.style.transition = "opacity .25s ease, transform .25s ease";
                flash.style.opacity = "0";
                flash.style.transform = "translateY(-4px)";
                window.setTimeout(function () {
                    flash.remove();
                }, 260);
            }, 5200);
        });

        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            form.addEventListener("submit", function (event) {
                var message = form.getAttribute("data-confirm") || "确定执行此操作吗？";
                if (!window.confirm(message)) {
                    event.preventDefault();
                }
            });
        });

        document.querySelectorAll("textarea[data-autogrow]").forEach(function (textarea) {
            var resize = function () {
                textarea.style.height = "auto";
                textarea.style.height = Math.min(textarea.scrollHeight, 180) + "px";
            };
            textarea.addEventListener("input", resize);
            resize();
        });
    });
})();
