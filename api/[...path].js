"use strict";

const { handle } = require("../router");

module.exports = function vercelHandler(req, res) {
  handle(req, res);
};