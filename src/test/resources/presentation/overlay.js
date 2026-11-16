
const id = "__security_demo_code_overlay__";

if (!document.documentElement) return;

let host = document.getElementById(id);

if (!host) {
    host = document.createElement("div");
    host.id = id;
    host.setAttribute("aria-hidden", "true");
    document.documentElement.appendChild(host);

    const root = host.attachShadow({ mode: "open" });

    const style = document.createElement("style");
    style.textContent = arguments[0];
    root.appendChild(style);

    /**
 * Creates a presentation overlay element with optional class and text content.
 *
 * @param {HTMLElement} parent the DOM element receiving the new children
 * @param {string} tag the HTML tag name
 * @param {string} className optional CSS classes
 * @param {string} text optional text content
 * @returns {HTMLElement} the computed result
 */
    const make = (parent, tag, className, text = "") => {
        const element = document.createElement(tag);
        if (className) element.className = className;
        element.textContent = text;
        parent.appendChild(element);
        return element;
    };

    const panel = make(root, "div", "panel");
    const top = make(panel, "div", "top");

    make(top, "span", "brand", "LIVE JAVA EXECUTION");
    make(top, "span", "action");

    make(panel, "div", "heading");

    const test = make(panel, "div", "section test");
    make(test, "div", "label", "TEST STEP");
    make(test, "pre", "code");

    const page = make(panel, "div", "section secondary page");
    make(page, "div", "label", "PAGE OBJECT");
    make(page, "pre", "code");

    make(panel, "div", "detail");
}

const root = host.shadowRoot;

if (root) {
    root.querySelector(".action").textContent = arguments[1];
    root.querySelector(".heading").textContent = arguments[2];
    root.querySelector(".test .label").textContent = arguments[3];
    root.querySelector(".test .code").textContent = arguments[4];
    root.querySelector(".page .label").textContent = arguments[5];
    root.querySelector(".page .code").textContent = arguments[6];

    root.querySelector(".page").style.display = arguments[6] ? "block" : "none";

    const detail = root.querySelector(".detail");
    detail.textContent = arguments[7];
    detail.style.display = arguments[7] ? "block" : "none";
}
