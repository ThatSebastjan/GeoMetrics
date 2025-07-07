/* 
    This file implements LLM API for AI generated summaries
*/

require("dotenv").config();

const AiInference = require("@azure-rest/ai-inference");
const { AzureKeyCredential } = require("@azure/core-auth");

const token = process.env["ACCESS_TOKEN"];

if(token == null){
	throw new Error("GitHub access token set as ACCESS_TOKEN environment variable is required for report summary generation!");
};

const endpoint = "https://models.github.ai/inference";
const model = "deepseek/DeepSeek-V3-0324";

const client = AiInference.default(endpoint, new AzureKeyCredential(token));


module.exports = {

    generateSummary: async (results, lotCenter, nearestWaterBody, nearestFireStation, lotElevation) => {

		nearestWaterBody = (nearestWaterBody != null) ? `${Math.round(nearestWaterBody.distance * 1000)}m` : "more than 5km";
		nearestFireStation = (nearestFireStation != null) ? `${nearestFireStation.distance.toFixed(2)}km` : "more than 100km";

		const prompt = [
			`Generate a very short (100 words max.) land lot assessment summary based on the following:`,
			`-Flood risk: ${results.floodRisk.toFixed(2)}%`,
			`-Landslide risk: ${results.landSlideRisk.toFixed(2)}%`,
			`-Earthquake risk: ${results.earthQuakeRisk.toFixed(2)}%`,
			`-Nearest body of water: ${nearestWaterBody}`,
			`-Nearest fire station: ${nearestFireStation}`,
			`-Lot elevation: ${lotElevation}m`,
			`Don't use any text formatting except newlines.`
		].join("\n");


		const response = await client.path("/chat/completions").post({
			body: {
				messages: [
					{ role: "system", content: "" },
					{ role: "user", content: prompt }
				],
				temperature: 0.8,
				top_p: 0.1,
				max_tokens: 2048,
				model: model
			}
		});

		if (AiInference.isUnexpected(response)) {
			console.log("Unexpected error in model response:", response.body.error);
			return "An error occurred :(";
		};

		return response.body.choices[0].message.content;
	},
};